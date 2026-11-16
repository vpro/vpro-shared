package nl.vpro.monitoring.binder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jspecify.annotations.NonNull;

import nl.vpro.util.ThreadPools;

/**
 * Exposes the JVM's Native Memory Tracking (NMT) summary (the same data {@code jcmd <pid> VM.native_memory summary}
 * prints) as Micrometer gauges, tagged by NMT category ({@code Thread}, {@code Code}, {@code Metaspace}, etc.), so
 * native/off-heap memory growth is visible in Prometheus without shelling into a running pod.
 * <p>
 * Requires the JVM to be started with {@code -XX:NativeMemoryTracking=summary} (or {@code detail}); if NMT is not
 * enabled, no metrics are registered.
 */
@Slf4j
public class NativeMemoryMetrics implements MeterBinder, Runnable, AutoCloseable {

    private static final ObjectName DIAGNOSTIC_COMMAND;
    static {
        try {
            DIAGNOSTIC_COMMAND = new ObjectName("com.sun.management:type=DiagnosticCommand");
        } catch (MalformedObjectNameException malformedObjectNameException) {
            throw new ExceptionInInitializerError(malformedObjectNameException.getMessage());
        }
    }

    private static final Pattern TOTAL = Pattern.compile("^Total:\\s*reserved=(\\d+)KB, committed=(\\d+)KB");

    // e.g. "-                 Java Heap (reserved=2805760KB, committed=178176KB)"
    // or   "-        Shared class space (reserved=16384KB, committed=13912KB, readonly=0KB)"
    // Nested detail lines (e.g. "(mmap: reserved=...)") start with '(' rather than a category name, so they
    // never match this (it requires a letter right after the optional leading '-').
    private static final Pattern CATEGORY = Pattern.compile("^-?\\s*([A-Za-z][A-Za-z ]*?)\\s*\\(reserved=(\\d+)KB, committed=(\\d+)KB");

    private final Duration interval;
    private final Map<String, AtomicLong> reserved = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> committed = new ConcurrentHashMap<>();
    private MeterRegistry registry;
    private ScheduledFuture<?> scheduledFuture;
    private boolean warnedNotEnabled = false;
    private boolean warnedUnparseable = false;

    public NativeMemoryMetrics() {
        this(Duration.ofSeconds(30));
    }

    public NativeMemoryMetrics(Duration interval) {
        this.interval = interval;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        this.registry = registry;
        run();
        if (!reserved.isEmpty()) {
            scheduledFuture = ThreadPools.backgroundExecutor.scheduleAtFixedRate(
                this, interval.getSeconds(), interval.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public synchronized void run() {
        String summary;
        try {
            summary = fetchSummary();
        } catch (Exception e) {
            log.debug("Could not obtain native memory summary: {}", e.toString());
            return;
        }
        update(summary);
    }

    synchronized void update(String summary) {
        if (summary == null || summary.contains("Native memory tracking is not enabled")) {
            if (!warnedNotEnabled) {
                log.info("Native Memory Tracking is not enabled (start the JVM with -XX:NativeMemoryTracking=summary to enable it). Not registering jvm.native.memory metrics");
                warnedNotEnabled = true;
            }
            return;
        }
        boolean matchedAny = false;
        for (String line : summary.split("\n")) {
            String trimmed = line.trim();
            Matcher totalMatcher = TOTAL.matcher(trimmed);
            if (totalMatcher.find()) {
                set("Total", Long.parseLong(totalMatcher.group(1)), Long.parseLong(totalMatcher.group(2)));
                matchedAny = true;
                continue;
            }
            Matcher matcher = CATEGORY.matcher(trimmed);
            if (matcher.find()) {
                set(matcher.group(1).trim(), Long.parseLong(matcher.group(2)), Long.parseLong(matcher.group(3)));
                matchedAny = true;
            }
        }
        if (!matchedAny && !warnedUnparseable) {
            log.warn("Native Memory Tracking is enabled, but its output could not be parsed into categories (JDK output format may have changed): {}", summary);
            warnedUnparseable = true;
        }
    }

    private void set(String category, long reservedKb, long committedKb) {
        reserved.computeIfAbsent(category, this::registerReserved).set(reservedKb * 1024);
        committed.computeIfAbsent(category, this::registerCommitted).set(committedKb * 1024);
    }

    private AtomicLong registerReserved(String category) {
        AtomicLong value = new AtomicLong();
        Gauge.builder("jvm.native.memory.reserved.bytes", value, AtomicLong::get)
            .tag("category", category)
            .description("Native memory reserved by the JVM, by Native Memory Tracking category")
            .baseUnit(BaseUnits.BYTES)
            .strongReference(true)
            .register(registry);
        return value;
    }

    private AtomicLong registerCommitted(String category) {
        AtomicLong value = new AtomicLong();
        Gauge.builder("jvm.native.memory.committed.bytes", value, AtomicLong::get)
            .tag("category", category)
            .description("Native memory committed by the JVM, by Native Memory Tracking category")
            .baseUnit(BaseUnits.BYTES)
            .strongReference(true)
            .register(registry);
        return value;
    }

    /**
     * Package-private so tests can stub a canned NMT summary without needing the JVM under test to actually
     * run with Native Memory Tracking enabled.
     */
    String fetchSummary() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        return (String) server.invoke(
            DIAGNOSTIC_COMMAND,
            "vmNativeMemory",
            new Object[]{new String[]{"summary"}},
            new String[]{"[Ljava.lang.String;"});
    }

    @Override
    public void close() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }
}

package nl.vpro.monitoring.binder;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;

import jakarta.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;


import org.natty.Parser;
import org.springframework.jmx.export.annotation.ManagedOperation;

import com.google.common.util.concurrent.AtomicDouble;

import nl.vpro.jmx.MBeans;
import nl.vpro.util.*;

@Slf4j
public class ScriptMeterBinder implements MeterBinder, Runnable, ScriptMeterMXBean {


    private final Map<String, AtomicDouble> cache = new ConcurrentHashMap<>();
    private final CommandExecutor commandExecutor;
    private Duration duration;
    private final String[] arguments;
    private MeterRegistry meterRegistry;
    private ScheduledFuture<?> scheduledFuture;
    private Duration interval;
    private final Set<Meter.Id> registered = new CopyOnWriteArraySet<>();

    ScriptMeterBinder(String name, Duration interval, CommandExecutor commandExecutor, Duration duration, String... arguments) {
        this.commandExecutor = commandExecutor;
        this.interval = interval;
        this.duration = duration;
        this.arguments = arguments;
        MBeans.registerBean(MBeans.getObjectNameWithName(this, name), this);
    }


    public ScriptMeterBinder(String interval, String[] executables,  String duration, String... arguments) {
        this(
            String.join(",", executables),
            TimeUtils.parseDuration(interval).orElse(Duration.ofHours(1)),
            new AbstractCommandExecutorWrapper() {
            @Override
            protected CommandExecutor getWrapped() {
                return CommandExecutorImpl.builder()
                    .executablesPaths(executables)
                    .optional(true)
                    .build();
            }
        }, TimeUtils.parseDuration(duration).orElse(Duration.ofDays(1)), arguments);
    }

    @Override
    public void bindTo(@NotNull MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.schedule();
    }

    private void schedule() {
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(false);
        }
        this.scheduledFuture = ThreadPools.backgroundExecutor.scheduleAtFixedRate(this, 0, interval.getSeconds(), TimeUnit.SECONDS);
    }



    private static final Parser PARSER = new Parser(TimeZone.getTimeZone(BindingUtils.DEFAULT_ZONE));

    @Override
    @ManagedOperation
    public synchronized void run() {
        try {
            String[] args = new String[this.arguments.length + 1];
            args[0] = String.valueOf(duration.toMinutes());
            System.arraycopy(this.arguments, 0, args, 1, this.arguments.length);
            if (commandExecutor.getBinary().get() != null) {
                log.info("Executing {} with {}", commandExecutor.getBinary(), Arrays.asList(args));
                commandExecutor.lines(args).forEach(l -> {
                        log.info(l);
                        ScriptGauge gauge = ScriptGauge.parse(l);
                        AtomicDouble atomic = cache.computeIfAbsent(gauge.key(), (k) -> {
                            Gauge numberOfEventsPerPeriod = Gauge.builder(gauge.name(), cache, c -> c.get(k).doubleValue())
                                .tags(gauge.tags())
                                .baseUnit(BaseUnits.EVENTS + "/" + duration)
                                .description("Number of events per " + duration)
                                .register(meterRegistry);
                            registered.add(numberOfEventsPerPeriod.getId());
                            return new AtomicDouble(0);
                        });

                        atomic.set(gauge.value());
                    }
                );
                log.debug("done");
            } else {
                log.info("Skipped {} {}", commandExecutor.getBinary(), Arrays.asList(args));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public String getDuration() {
        return duration.toString();
    }

    @Override
    public void setDuration(String duration) {
        Duration parsed = TimeUtils.parseDuration(duration).orElse(this.duration);
        if (! parsed.equals(this.duration)) {
            this.duration = parsed;
            this.registered.forEach(i -> meterRegistry.remove(i));
            cache.clear();
        }
    }

    @Override
    public String getScheduleInterval() {
        return interval.toString();
    }

    @Override
    public void setScheduleInterval(String duration) {
        Duration parsed = TimeUtils.parseDuration(duration).orElse(this.interval);
        if (! parsed.equals(this.interval)) {
            this.interval = parsed;
            schedule();
        }

        }
}

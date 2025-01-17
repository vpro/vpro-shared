package nl.vpro.monitoring.binder;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.natty.Parser;

import com.google.common.util.concurrent.AtomicDouble;

import nl.vpro.util.*;

@Slf4j

public class ScriptMeterBinder implements MeterBinder, Runnable {


    private final Map<String, AtomicDouble> CACHE = new ConcurrentHashMap<>();
    private final CommandExecutor commandExecutor;
    private final String[] arguments;
    private MeterRegistry meterRegistry;

    ScriptMeterBinder(CommandExecutor commandExecutor, String... arguments) {
        this.commandExecutor = commandExecutor;
        this.arguments = arguments;
    }


    public ScriptMeterBinder(String[] executables,  String... arguments) {
        this(new AbstractCommandExecutorWrapper() {
            @Override
            protected CommandExecutor getWrapped() {
                return CommandExecutorImpl.builder()
                    .executablesPaths(executables)
                    .build();
            }
        }, arguments);
    }

    @Override
    public void bindTo(@NotNull MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        ThreadPools.backgroundExecutor.scheduleAtFixedRate(this, 0, 1, TimeUnit.HOURS);
    }


    private static final Parser PARSER = new Parser(TimeZone.getTimeZone(BindingUtils.DEFAULT_ZONE));

    @Override
    public synchronized void run() {
        String[] args = Stream.of(arguments).map(a -> {
            if (a.startsWith("NATTY:")){
                a = DateUtils.toInstant(PARSER.parse(a.substring("NATTY:".length())).get(0).getDates().get(0)).toString();
            }
            return a;
        }).toArray(String[]::new);
        commandExecutor.lines(args).forEach(l -> {
                ScriptGauge gauge = ScriptGauge.parse(l);
                AtomicDouble atomic = CACHE.computeIfAbsent(gauge.key(), (k) -> {
                    Gauge.builder(gauge.name(), CACHE, c -> c.get(k).doubleValue())
                        .tags(gauge.tags())
                        .baseUnit(BaseUnits.EVENTS)
                        .register(meterRegistry);
                    return new AtomicDouble(0);
                });
                atomic.set(gauge.value());
            }
        );
    }
}

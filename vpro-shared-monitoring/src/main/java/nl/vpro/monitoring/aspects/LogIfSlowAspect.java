package nl.vpro.monitoring.aspects;


import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import nl.vpro.logging.aspects.LogIfSlow;


/**
 * Works together with {@link LogIfSlow} annotation. All (spring managed) methods which are annotated with that annotation
 * are measured, and a warning or error is issued if they're suspiciously slow.
 *
 * @author Michiel Meeuwissen
 */
@Aspect
@Component
public class LogIfSlowAspect {

    /**
     * The thresholds are given as duration strings, which may contain spring EL placeholder. So they require some placeholder resolving and parsing. This is cached in this.
     */
    private static final Map<String, Long> DURATION_CACHE = new ConcurrentHashMap<>();

    /**
     * {@link LogManager#getLogger(String)} is not free (it resolves a {@link org.apache.logging.log4j.spi.LoggerContext}),
     * and the outcome is invariant per advised method, so it's cached here.
     */
    private static final Map<Class<?>, Logger> LOGGER_BY_CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Logger> LOGGER_BY_NAME_CACHE = new ConcurrentHashMap<>();


    @Autowired
    private MeterRegistry meteringRegistry;

    @Autowired
    private Environment env;

    @Around(value = "@annotation(logIfSlow)")
    public Object logSlow(
        ProceedingJoinPoint joinPoint,
        LogIfSlow logIfSlow) throws Throwable {

        final String loggerName  = logIfSlow.logger();
        final Logger logger = "".equals(loggerName) ?
            LOGGER_BY_CLASS_CACHE.computeIfAbsent(joinPoint.getTarget().getClass(), LogManager::getLogger) :
            LOGGER_BY_NAME_CACHE.computeIfAbsent(loggerName, LogManager::getLogger);
        final long start = System.nanoTime();
        try {
            return joinPoint.proceed(joinPoint.getArgs());

        } finally {
            final Duration duration = Duration.ofNanos(System.nanoTime() - start).plusNanos(500_000).truncatedTo(ChronoUnit.MILLIS);
            final long durationInMillis = duration.toMillis();
            final long errorThreshold = DURATION_CACHE.computeIfAbsent(logIfSlow.errorThreshold(), s -> LogIfSlowAspect.this.parse(logger, s));
            final long warnThreshold = "".equals(logIfSlow.warnThreshold()) ? errorThreshold / 2 :
                DURATION_CACHE.computeIfAbsent(logIfSlow.warnThreshold(), s -> LogIfSlowAspect.this.parse(logger, s)
                );
            final Level level;
            if (durationInMillis > errorThreshold) {
                level = Level.ERROR;
            } else if (durationInMillis > warnThreshold) {
                level = Level.WARN;
            } else {
                level = Level.DEBUG;
            }

            // Recorded unconditionally (not just for WARN/ERROR), so the timer reflects the full latency
            // distribution rather than only the outliers that also happened to get logged.
            meteringRegistry.timer("method.duration",
                "class", joinPoint.getTarget().getClass().getName(),
                "method", joinPoint.getSignature().getName(),
                "level", level.toString()
            ).record(duration);

            logger.log(level, "{} ({}) took {}",
                () -> joinPoint.getSignature().toShortString(),
                joinPoint::getArgs,
                () -> duration
            );
        }
    }

    protected Long parse(Logger log, String s) {
        Duration d = Duration.parse(env.resolveRequiredPlaceholders(s));
        log.info("Resolved {} -> {}", s, d);
        return d.toMillis();
    }
}

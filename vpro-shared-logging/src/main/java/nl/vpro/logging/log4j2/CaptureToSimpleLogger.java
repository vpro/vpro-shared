package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import nl.vpro.logging.simple.Level;
import nl.vpro.logging.simple.SimpleLogger;

/**
 * Simple setup using log4j2 to temporarily capture logging and collect it to a SimpleLogger.
 * <p>
 * usage
 * <pre>{@code
 *
 *   try (CaptureToSimpleLogger capture = CaptureToSimpleLogger.of(simpleLogger)) {
 *
 *       stuff, which might log some things (as long as in same thread!)
 *
 *       publication.setMessages(capture.get())
 *   }
 * }
 * </pre>
 *
 *
 * @author Michiel Meeuwissen
 * @since 5.11
 */
@Log4j2
public class CaptureToSimpleLogger implements AutoCloseable {

    static final ThreadLocal<UUID> threadLocal = ThreadLocal.withInitial(() -> null);

    private final AbstractAppender appender;


    public static CaptureToSimpleLogger of(SimpleLogger simpleLogger) {
        return CaptureToSimpleLogger.builder()
            .simpleLogger(simpleLogger)
            .build();
    }

    @lombok.Builder
    private CaptureToSimpleLogger(
        SimpleLogger simpleLogger,
        final String name) {
        final UUID uuid = UUID.randomUUID();
        threadLocal.set(uuid);
        appender = new AbstractAppender(uuid.toString(), new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    boolean inThread = uuid.equals(threadLocal.get());
                    return inThread ? Result.NEUTRAL : Result.DENY;
                }
            }, null, false, null) {
            @Override
            public void append(LogEvent event) {
                String m = event.getMessage().getFormattedMessage();
                simpleLogger.accept(
                    Level.valueOf(event.getLevel().name()),
                    m,
                    event.getThrown()
                );
            }
        };
        if (LogManager.getRootLogger() instanceof Logger log4j) {
            synchronized (LogManager.class) {
                appender.start();
                assert appender.isStarted() : "Appender is not started";
                log4j.addAppender(appender);
                log4j.getContext().updateLoggers(); // ensure the logger is updated with the new appender
            }
        } else {
            log.info("Current logging implementation is not log4j2-core");
        }
    }

    @Override
    public void close() {
        threadLocal.remove();
        if (LogManager.getRootLogger() instanceof Logger log4j) {
            synchronized (LogManager.class) {
                log4j.removeAppender(appender);
                log4j.getContext().updateLoggers();
                appender.stop();
            }
        }
    }



}

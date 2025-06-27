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
 * Simple setup using log4j2 to temporarily capture logging and collect it to a String.
 * <p>
 * usage
 * <pre>{@code
 *
 *   try (CaptureStringFromLogger capture = new CaptureStringFromLogger()) {
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
 */
@Log4j2
public class CaptureToSimpleLogger implements AutoCloseable {

    static final ThreadLocal<UUID> threadLocal = ThreadLocal.withInitial(() -> null);

    private final SimpleLogger logger;
    private final AbstractAppender appender;


    @lombok.Builder
    private CaptureToSimpleLogger(
        SimpleLogger simpleLogger,
        final String name) {
        final UUID uuid = UUID.randomUUID();
        threadLocal.set(uuid);
        this.logger = simpleLogger;
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
        if (LogManager.getRootLogger() instanceof Logger logger) {
            synchronized (CaptureToSimpleLogger.class) {


                appender.start();
                assert appender.isStarted() : "Appender is not sarted";
                logger.addAppender(appender);
                logger.getContext().updateLoggers(); // ensure the logger is updated with the new appender
                }

        } else {
            log.info("Current logging implementation is not log4j2-core");
        }
    }

    @Override
    public void close() {
        threadLocal.remove();
        if (LogManager.getRootLogger() instanceof Logger logger) {
            synchronized (CaptureToSimpleLogger.class) {

                logger.removeAppender(appender);
                logger.getContext().updateLoggers(); // ensure the logger is updated with the new appender
            }

        }
    }



}

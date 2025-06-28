package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**

 *
 * @author Michiel Meeuwissen
 * @since 5.11
 */
@Log4j2
public abstract  class AbstractCaptureLogger  implements AutoCloseable, Consumer<LogEvent> {
    static final ThreadLocal<UUID> threadLocal = ThreadLocal.withInitial(() -> null);

    static final Map<UUID, Consumer<LogEvent>> LOGGERS = new ConcurrentHashMap<>();

    static AbstractAppender appender ;

    static private  synchronized void checkAppend() {
        if (appender == null) {
            if (LogManager.getRootLogger() instanceof Logger log4j) {
                appender = new AbstractAppender(AbstractCaptureLogger.class.getName(), new AbstractFilter() {
                    @Override
                    public Result filter(LogEvent event) {
                        UUID uuid = threadLocal.get();
                        boolean inThread = uuid != null && LOGGERS.containsKey(uuid);
                        return inThread ? Result.NEUTRAL : Result.DENY;
                    }
                }, null, false, null) {
                    @Override
                    public void append(LogEvent event) {
                        UUID uuid = threadLocal.get();
                        Consumer<LogEvent> simpleLogger = LOGGERS.get(uuid);
                        if (simpleLogger != null) {
                            simpleLogger.accept(event);
                        }
                    }

                };

                appender.start();
                log4j.addAppender(appender);
                log4j.getContext().updateLoggers(); // ensure the logger is updated with the new appender

            } else {
                log.info("Current logging implementation is not log4j2-core");
            }
        }
    }


    protected final UUID uuid;

    AbstractCaptureLogger() {
        this.uuid = UUID.randomUUID();
        threadLocal.set(uuid);
        LOGGERS.put(uuid, this);
        checkAppend();
    }

    @Override
    public void close() {
        if (LogManager.getRootLogger() instanceof Logger log4j) {
            var uuid = threadLocal.get();
            threadLocal.remove();
            LOGGERS.remove(uuid);

        }
    }

}

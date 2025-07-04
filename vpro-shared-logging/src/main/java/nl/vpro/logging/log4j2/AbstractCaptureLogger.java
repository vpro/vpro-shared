package nl.vpro.logging.log4j2;

import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
public abstract class AbstractCaptureLogger  implements AutoCloseable  {

    static final ThreadLocal<UUID> THREAD_LOCAL = ThreadLocal.withInitial(() -> null);
    static final Map<UUID,  AbstractCaptureLogger> LOGGERS = new ConcurrentHashMap<>();

    static AbstractAppender appender ;

    static private synchronized void checkAppender() {
        if (appender == null) {
            if (LogManager.getRootLogger() instanceof Logger log4j) {
                appender = new AbstractAppender(AbstractCaptureLogger.class.getName(), new AbstractFilter() {
                    @Override
                    public Result filter(LogEvent event) {
                        UUID uuid = THREAD_LOCAL.get();
                        boolean inThread = uuid != null && LOGGERS.containsKey(uuid);
                        return inThread ? Result.NEUTRAL : Result.DENY;
                    }
                }, null, false, null) {
                    @Override
                    public void append(LogEvent event) {
                        UUID uuid = THREAD_LOCAL.get();
                        AbstractCaptureLogger consumer = LOGGERS.get(uuid);
                        if (consumer != null) {
                            consumer.accept(event);
                        }
                    }
                };
                appender.start();
                log4j.addAppender(appender);
                log4j.getContext().updateLoggers(); // ensure the logger is updated with the new appender
                log.info("Added appender {} to {} to arrange log capturing", appender.getName(), log4j.getName());
            } else {
                log.info("Current logging implementation is not log4j2-core");
            }
        }
    }


    protected final UUID uuid = UUID.randomUUID();

    AbstractCaptureLogger() {
        checkAppender();
        THREAD_LOCAL.set(uuid);
        LOGGERS.put(uuid, this);
    }

    protected abstract void accept(LogEvent event);

    @Override
    public void close() {
        if (LogManager.getRootLogger() instanceof Logger log4j) {
            var uuid = THREAD_LOCAL.get();
            THREAD_LOCAL.remove();
            LOGGERS.remove(uuid);
        }
    }

}

package nl.vpro.logging.log4j2;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.meeuw.functional.ThrowAnyAutoCloseable;

/**

 *
 * @author Michiel Meeuwissen
 * @since 5.11
 */
@Log4j2
public abstract class AbstractCaptureLogger  implements AutoCloseable {

    static final ThreadLocal<UUID> THREAD_LOCAL = ThreadLocal.withInitial(() -> null);
    static final Map<UUID,  AbstractCaptureLogger> LOGGERS = new ConcurrentHashMap<>();
    static final Map<UUID,  AbstractCaptureLogger> ALL_LOGGERS = new ConcurrentHashMap<>();


    static AbstractAppender currentThreadAppender ;
    static AbstractAppender allAppender ;

    static private synchronized void checkAppender(boolean currentThreadOnly) {
        if (LogManager.getRootLogger() instanceof Logger log4j) {
            if (currentThreadOnly) {
                if (currentThreadAppender == null) {
                    currentThreadAppender = createAppender(log4j, currentThreadOnly);
                }
            } else {
                if (allAppender == null) {
                    allAppender = createAppender(log4j, false);
                }
            }
        } else {
            log.warn("Current logging implementation is not log4j2-core");

        }
    }

    private static AbstractAppender createAppender(Logger log4j, boolean currentThreadOnly) {
        AbstractAppender  appender = new AbstractAppender(AbstractCaptureLogger.class.getName() + "." + currentThreadOnly, new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                if (currentThreadOnly) {
                    UUID uuid = THREAD_LOCAL.get();
                    boolean inThread = uuid != null && LOGGERS.containsKey(uuid);
                    return inThread ? Result.NEUTRAL : Result.DENY;
                } else {
                    return Result.NEUTRAL;
                }
            }
        }, null, false, null) {
            @Override
            public void append(LogEvent event) {
                if (currentThreadOnly) {
                    UUID uuid = THREAD_LOCAL.get();
                    AbstractCaptureLogger consumer = LOGGERS.get(uuid);
                    if (consumer != null) {
                        consumer.accept(event);
                    }
                } else {
                    for (AbstractCaptureLogger consumer: ALL_LOGGERS.values()) {
                        consumer.accept(event);

                    }
                }
            }
        };
        log.info("Added appender {} to {} to arrange log capturing", appender.getName(), StringUtils.isBlank(log4j.getName()) ? "<root logger>" : log4j.getName());
        appender.start();
        log4j.addAppender(appender);
        log4j.getContext().updateLoggers(); // ensure the logger is updated with the new appender

        return appender;
    }



    @Getter
    protected final UUID uuid;
    private final boolean currentThreadOnly;

    AbstractCaptureLogger(UUID uuid, boolean currentThreadOnly) {
        checkAppender(currentThreadOnly);
        this.currentThreadOnly = currentThreadOnly;
        this.uuid = uuid;
        THREAD_LOCAL.set(uuid);
        if (currentThreadOnly) {
            LOGGERS.put(uuid, this);
        } else {
            ALL_LOGGERS.put(uuid, this);

        }
    }

    AbstractCaptureLogger(boolean currentThreadOnly) {
        this(UUID.randomUUID(), currentThreadOnly);
    }

    /**
     * Associates this capturing logger with the current thread. This happens automatically in the constructor, but you can call it again if the instance happens to be used in a different thread later.
     * @see #disassociate()
     */
    public ThrowAnyAutoCloseable associateWithCurrentThread() {
        THREAD_LOCAL.set(uuid);
        return this::disassociate;
    }

    /**
     * Disassociates this capturing logger from the current thread.
     */
    public void disassociate() {
        THREAD_LOCAL.remove();
    }


    protected abstract void accept(LogEvent event);

    @SuppressWarnings("resource")
    @Override
    public void close() {
        disassociate();
        if (LogManager.getRootLogger() instanceof Logger log4j) {
            if (currentThreadOnly) {
                LOGGERS.remove(uuid);
            } else {
                ALL_LOGGERS.remove(uuid);
            }
        }
    }

}

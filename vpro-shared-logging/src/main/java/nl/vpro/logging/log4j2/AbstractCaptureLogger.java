package nl.vpro.logging.log4j2;

import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.ThrowAnyAutoCloseable;

import static org.meeuw.functional.Predicates.alwaysTrue;

/**

 *
 * @author Michiel Meeuwissen
 * @since 5.11
 */
@Log4j2
public abstract class AbstractCaptureLogger  implements AutoCloseable {

    static final ThreadLocal<Set<UUID>> THREAD_LOCAL = ThreadLocal.withInitial(HashSet::new);
    static final Map<UUID,  AbstractCaptureLogger> LOGGERS = new ConcurrentHashMap<>();
    static final Map<UUID,  AbstractCaptureLogger> ALL_LOGGERS = new ConcurrentHashMap<>();

    static volatile AbstractAppender currentThreadAppender ;
    static volatile AbstractAppender allAppender ;

    static private synchronized void checkAppender(boolean currentThreadOnly) {

        if (currentThreadOnly) {
            if (currentThreadAppender == null) {
                if (LogManager.getRootLogger() instanceof Logger log4j) {
                    currentThreadAppender = createAppender(log4j, true);
                    return;
                }
            } else {
                return;
            }
        } else {
            if (allAppender == null) {
                if (LogManager.getRootLogger() instanceof Logger log4j) {
                    allAppender = createAppender(log4j, false);
                    return;
                }
            } else {
                return;
            }
        }
        log.warn("Current logging implementation is not log4j2-core");
    }

    @SneakyThrows
    private static AbstractAppender createAppender(Logger log4j, boolean currentThreadOnly) {
        AbstractAppender  appender = new AbstractAppender(AbstractCaptureLogger.class.getName() + "." + currentThreadOnly, new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                if (currentThreadOnly) {
                    Set<UUID> uuid = THREAD_LOCAL.get();
                    boolean inThread = LOGGERS.keySet().stream().anyMatch(uuid::contains);
                    return inThread ? Result.NEUTRAL : Result.DENY;
                } else {
                    return Result.NEUTRAL;
                }
            }
        }, null, false, null) {
            @Override
            public void append(LogEvent event) {
                if (isFiltered(event)) {
                    return;
                }
                if (currentThreadOnly) {
                    Set<UUID> uuids = THREAD_LOCAL.get();
                    for (UUID uuid: uuids) {
                        AbstractCaptureLogger consumer = LOGGERS.get(uuid);
                        if (consumer != null) {
                            if (consumer.filter.test(event)) {
                                consumer.accept(event);
                            }
                        } else {
                            log.debug("No consumer for {} and event {}", uuid, event.getMessage().getFormattedMessage());
                        }
                    }
                } else {
                    for (AbstractCaptureLogger consumer: ALL_LOGGERS.values()) {
                        if (consumer.filter.test(event)) {
                            consumer.accept(event);
                        }
                    }
                }
            }
        };
        log.debug("Added appender {} to {} to arrange log capturing", appender.getName(), StringUtils.isBlank(log4j.getName()) ? "<root logger>" : log4j.getName());
        appender.start();
        Thread.sleep(100);

        log4j.addAppender(appender);
        log4j.getContext().updateLoggers(); // ensure the logger is updated with the new appender

        return appender;
    }


    public static Predicate<LogEvent> levelFilter(Level level) {
        if (level == null) {
            return alwaysTrue();
        }
        return event -> event.getLevel().isMoreSpecificThan(level);
    }

    public static Predicate<LogEvent> loggerNameFilter(String loggerName) {
        if (loggerName == null) {
            return alwaysTrue();
        }
        final String plusDot = loggerName + ".";
        return event -> event.getLoggerName().equals(loggerName) || event.getLoggerName().startsWith(plusDot);
    }



    public static Predicate<LogEvent> filter(Predicate<LogEvent> predicate, Level level, String loggerName, Class<?> loggerClass) {
        Predicate<LogEvent> result = null;
        if (predicate != null) {
            result = predicate;
        }
        if (level != null) {
            result = result == null ? levelFilter(level) : result.and(levelFilter(level));
        }
        if (loggerClass != null) {
            if (loggerName != null) {
                throw new IllegalArgumentException("Cannot specify both loggerName and loggerClass");
            }
            loggerName = loggerClass.getName();

        }
        if (loggerName != null) {
            result = result == null ? loggerNameFilter(loggerName) : result.and(loggerNameFilter(loggerName));
        }
        return result == null ? alwaysTrue() : result;
    }


    @Getter
    protected final @NonNull UUID uuid;
    @Getter
    private final boolean currentThreadOnly;
    private final Predicate<LogEvent> filter;



    AbstractCaptureLogger(@Nullable Predicate<LogEvent> filter, @Nullable UUID uuid, boolean currentThreadOnly) {
        this.filter = filter == null ? alwaysTrue() : filter;
        checkAppender(currentThreadOnly);
        this.currentThreadOnly = currentThreadOnly;
        this.uuid = uuid == null ? UUID.randomUUID() : uuid;
        THREAD_LOCAL.get().add(this.uuid);
        if (currentThreadOnly) {
            LOGGERS.put(this.uuid, this);
        } else {
            ALL_LOGGERS.put(this.uuid, this);

        }
    }

    AbstractCaptureLogger(Predicate<LogEvent>  level, boolean currentThreadOnly) {
        this(level, null, currentThreadOnly);
    }

    /**
     * Associates this capturing logger with the current thread. This happens automatically in the constructor, but you can call it again if the instance happens to be used in a different thread later.
     * @see #disassociate()
     */
    public ThrowAnyAutoCloseable associateWithCurrentThread() {
        THREAD_LOCAL.get().add(uuid);
        return this::disassociate;
    }

    /**
     * Disassociates this capturing logger from the current thread.
     */
    public void disassociate() {
        THREAD_LOCAL.get().remove(uuid);
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

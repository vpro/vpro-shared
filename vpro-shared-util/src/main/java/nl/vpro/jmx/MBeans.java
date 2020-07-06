package nl.vpro.jmx;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.management.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import com.google.common.util.concurrent.Futures;

import nl.vpro.logging.Slf4jHelper;
import nl.vpro.logging.simple.Slf4jSimpleLogger;
import nl.vpro.logging.simple.StringBuilderSimpleLogger;
import nl.vpro.logging.simple.StringSupplierSimpleLogger;
import nl.vpro.util.ThreadPools;

/**
 * Utilities to start jmx tasks in the background.
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class MBeans {

    static final Map<String, LockValue> locks = new ConcurrentHashMap<>();

    public static final Duration DEFAULT_DURATION =  Duration.ofSeconds(5);

    public static boolean isRunning(final String key) {
        return locks.containsKey(key);
    }


    public static Future<String>  cancel(final String key){
        LockValue future = locks.get(key);
        if (future == null) {
            return Futures.immediateFuture("Not running");
        }
        future.cancel();
        // should not be needed, because happening in finally, but if the called code does reuse to shut down propery, then simply abandon it.
        ScheduledFuture<String> schedule = ThreadPools.backgroundExecutor.schedule(() -> {
                LockValue abandoned = locks.remove(key);
                if (abandoned != null) {
                    log.warn("abandonded {}", abandoned);
                    return "Abandonded";
                }
                return "Canceled";
            },
            2,
            TimeUnit.SECONDS
        );
        return schedule;
    }



    /**
     *
     * @param key A key on which the job can be 'locked'.
     * @param description A supplier that before the job starts should describe the job. One can be created using e.g. {@link #multiLine(Logger, String, Object...)} or {@link #singleLine(Logger, String, Object...)}
     * @param wait How long to wait before returing with a message that the job is not yet finished, but still running.
     * @param logger A job returning a String when ready. This string will be returned.
     * @return The string to be used as a return value for a JMX operation
     */
    public static String returnString(
        @Nullable final String key,
        @NonNull final StringSupplierSimpleLogger description,
        @NonNull final Duration wait,
        @NonNull final Consumer<StringSupplierSimpleLogger> logger) {
        if (key != null) {
            if (isRunning(key)) {
                return "Job " + key + " is still running, so could not be started again with " + description.get();
            }
        }
        LockValue value = new LockValue(description);
        if (key != null) {
            locks.put(key, value);
        }
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            final String threadName = Thread.currentThread().getName();
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(MBeans.class.getClassLoader());
                Thread.currentThread().setName(threadName + ":" + description.get());
                value.setThread(Thread.currentThread());
                logger.accept(description);
            } catch (Exception e) {
                description.error(e.getClass().getName() + " " + e.getMessage(), e);
            } finally {
                if (key != null) {
                    locks.remove(key);
                }
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                Thread.currentThread().setName(threadName);
            }
            return description.get();

        });
        value.setFuture(future);

        try {
            return future.get(wait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            log.info(ie.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.info(e.getMessage());
        } catch (TimeoutException e) {
            return description.get() + "\n...\nstill busy. Please check logs";
        }
        return description.get();
    }


    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)), with no key (meaning that jobs can be started concurrently.
     */
    public static String returnString(
        @NonNull StringSupplierSimpleLogger description,
        @NonNull Duration wait,
        @NonNull Consumer<StringSupplierSimpleLogger> logger) {
        return returnString(null, description, wait, logger);
    }

    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)), with no key (meaning that jobs can be started concurrently.
     */
    public static String returnString(
        @NonNull StringSupplierSimpleLogger description,
        @NonNull Consumer<StringSupplierSimpleLogger> logger) {
        return returnString(null, description, DEFAULT_DURATION, logger);
    }

    public static String returnMultilineString(
        @NonNull Logger log,
        @NonNull Consumer<StringSupplierSimpleLogger> logger) {
        return returnString(multiLine(log), logger);
    }



    @Deprecated
    public static String returnString(
        @Nullable final String key,
        @NonNull final StringSupplierSimpleLogger description,
        @NonNull final  Duration wait,
        @NonNull final Callable<String> job) {
        return returnString(key, description, wait, l -> {
            try {
                String s = job.call();
                l.info(s);
            } catch (Exception e) {
                description.error(e.getClass().getName() + " " + e.getMessage(), e);
            }
        }
        );
      }

    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Callable)), with no key (meaning that jobs can be started concurrently.
     */
    @Deprecated
    public static String returnString(
        @NonNull StringSupplierSimpleLogger description,
        @NonNull Duration wait,
        @NonNull Callable<String> job) {
        return returnString(null, description, wait, job);
    }


    @Deprecated
    public static String returnString(
        @NonNull Supplier<String> description,
        @NonNull Duration wait,
        @NonNull Callable<String> job) {
        return returnString(null, singleLine(log, description.get()), wait, job);
    }
    /**
     * Defaulting version of {@link #returnString(StringSupplierSimpleLogger, Duration, Callable)} waiting for 5 seconds before time out.*/
    @Deprecated
    public static String returnString(
        @NonNull StringSupplierSimpleLogger description,
        @NonNull Callable<String> job) {
        return returnString(description, DEFAULT_DURATION, job);
    }


    /**
     * @param log Logger instance to log too
     * @param message Initial value of the string
     * @param args The arguments of the initial value
     * @return a {@link StringBuilderSimpleLogger} representing a single line string (actually a {@link UpdatableString}
     */
    public static StringSupplierSimpleLogger singleLine(Logger log, String message, Object... args) {
        return new UpdatableString(log, message, args);
    }


    /**
     * @param log Logger instance to log too
     * @param message First line of the string (logged as info)
     * @param args The arguments of the first line
     * @return a {@link StringBuilderSimpleLogger} representing multiple lines actually a {@link StringBuilderSimpleLogger}
     */
    public static StringSupplierSimpleLogger multiLine(Logger log, String message, Object... args) {

        StringSupplierSimpleLogger string  = StringBuilderSimpleLogger.builder()
            .prefix((l) -> l.compareTo(Level.ERROR) < 0 ? "" : l.name() + " ")
            .chain(Slf4jSimpleLogger.of(log));
        if (message != null) {
            string.info(message, args);
        }
        return string;
    }

    /**
     * @param log Logger instance to log too
     * @return a {@link StringBuilderSimpleLogger} representing multiple lines actually a {@link StringBuilderSimpleLogger}
     */
    public static StringSupplierSimpleLogger multiLine(Logger log) {
        return multiLine(log, null);
    }
    /**
     * A String supplier of one line. This can be used as argument for {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)}
     */
    public static class UpdatableString implements StringSupplierSimpleLogger {
        private final Logger logger;
        private CharSequence string;

        public UpdatableString(Logger logger, String string, Object... args) {
            this.logger = logger;
            info(string, args);
        }

        @Override
        public String get() {
            return toString();
        }

        @Override
        public void accept(Level level, CharSequence s, Throwable t) {
            CharSequence prev = this.string;
            this.string = s;
            if (logger != null) {
                if (! Objects.equals(prev, this.string)) {
                    Slf4jHelper.log(logger, level, this.string.toString());
                }
            }
        }

        @Override
        public String toString() {
            return string.toString();
        }
    }


    /**
     * @since 2.7
     */
    public static boolean isEmpty(String string) {
        return "String".equals(string) || StringUtils.isEmpty(string);
    }

    /**
     * @since 2.7
     */
    public static boolean isBlank(String string) {
        return "String".equals(string) || StringUtils.isBlank(string);
    }

    /**
     * @since 2.10
     */
    public static synchronized void registerBean(ObjectName name, Object object) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            unregister(name);
            mbs.registerMBean(object, name);
        } catch (NotCompliantMBeanException | MBeanRegistrationException | InstanceAlreadyExistsException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     *
     */
    @SneakyThrows
    public static synchronized ObjectName registerBean(Class<?> clazz, String name,  Object object) {
        ObjectName objectName = new ObjectName(clazz.getPackage().getName() + ":name=" + name + ",type=" + clazz.getSimpleName());
        registerBean(objectName, object);
        return
    }


    /**
     * @since 2.10
     */
    public static void unregister(ObjectName name) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            if (mbs.isRegistered(name)) {
                log.debug("Unregistering mbean {}", name);
                try {
                    mbs.unregisterMBean(name);
                } catch (InstanceNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.debug("Not registered");
            }
        } catch (MBeanRegistrationException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @since 2.10
     */
    public static ObjectName getObjectName(String prefix, Object object) {
        try {
            String mbeanName = object.getClass().getSimpleName();
            return new ObjectName(prefix + "?type=" + mbeanName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    @Getter
    @Setter
    public static class LockValue {
        Future<?> future;
        Thread thread;

        @NonNull
        Supplier<String> description;

        private LockValue(Supplier<String> description) {
            this.description = description;
        }

        public void cancel() {
            if (future != null){
                future.cancel(true);
            }
            if (thread != null) {
                thread.interrupt();
            }
        }
        @Override
        public String toString() {
            return description.get() + ":" + future;
        }
    }

}

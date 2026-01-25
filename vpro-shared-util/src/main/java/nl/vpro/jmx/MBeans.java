package nl.vpro.jmx;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.management.*;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.ThrowAnyConsumer;
import org.slf4j.Logger;

import nl.vpro.logging.Slf4jHelper;
import nl.vpro.logging.simple.*;
import nl.vpro.util.ImmediateFuture;
import nl.vpro.util.ThreadPools;

/**
 * Utilities to start jmx tasks in the background.
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class MBeans {

    private static final ExecutorService EXECUTOR =
        Executors.newCachedThreadPool
            (ThreadPools.createThreadFactory("MBeans", true, Thread.MAX_PRIORITY));

    static final Map<String, LockValue> locks = new ConcurrentHashMap<>();

    public static final Duration DEFAULT_DURATION =  Duration.ofSeconds(5);

    public static boolean isRunning(final String key) {
        return locks.containsKey(key);
    }


    public static Future<String> cancel(final String key){
        LockValue future = locks.get(key);
        if (future == null) {
            return new ImmediateFuture<>("Not running");
        }
        future.cancel();
        // should not be needed, because happening in finally, but if the called code does refuse to shut down properly, then simply abandon it.

        return ThreadPools.backgroundExecutor.schedule(() -> {
                final LockValue abandoned = locks.remove(key);
                if (abandoned != null) {
                    log.warn("abandoned {}", abandoned);
                    return "Abandoned " + abandoned;
                }
                return "Canceled";
            },
            2,
            TimeUnit.SECONDS
        );
    }



    /**
     *
     * @param key A key on which the job can be 'locked'.
     * @param stringSupplierLogger  A logger that will produce the resulting string. One can be created using e.g. {@link #multiLine(Logger, String, Object...)} or {@link #singleLine(Logger, String, Object...)}
     * @param wait How long to wait before returning with a message that the job is not yet finished, but still running. This may be {@code null}, in which case this will run nothing in the background
     * @param job A job to run. It is a consumer of a {@link SimpleLogger}, to which it can log its progress.
     * @return The string to be used as a return value for a JMX operation
     */
    public static String returnString(
        @Nullable final String key,
        @NonNull  final StringSupplierSimpleLogger stringSupplierLogger,
        @Nullable final Duration wait,
        @NonNull  final Consumer<StringSupplierSimpleLogger> job) {
        if (key != null) {
            if (isRunning(key)) {
                return "Job " + key + " is still running, so could not be started again with " + stringSupplierLogger.get();
            }
        }
        final LockValue value = new LockValue(stringSupplierLogger);
        if (key != null) {
            locks.put(key, value);
        }

        Supplier<String> supplier = () -> {
            try {
                value.setThread(Thread.currentThread());
                job.accept(stringSupplierLogger);
            } catch (Exception e) {
                stringSupplierLogger.error(e.getClass().getName() + " " + e.getMessage(), e);
            } finally {
                if (key != null) {
                    locks.remove(key);
                }
            }
            return stringSupplierLogger.get();

        };
        if (wait != null) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                final String threadName = Thread.currentThread().getName();
                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(MBeans.class.getClassLoader());
                    Thread.currentThread().setName(threadName + ":" + stringSupplierLogger.get());
                    return supplier.get();
                } finally {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                    Thread.currentThread().setName(threadName);
                }
            }, EXECUTOR);
            value.setFuture(future);

            try {
                return future.get(wait.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                log.info(ie.getMessage());
                Thread.currentThread().interrupt();
                return stringSupplierLogger.get();
            } catch (ExecutionException e) {
                log.info(e.getMessage());
                return stringSupplierLogger.get();
            } catch (TimeoutException e) {
                return stringSupplierLogger.get() + "\n...\nstill busy. Please check logs";
            }
        } else {
            return supplier.get();
        }
    }



    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)}, with a duration {@link #DEFAULT_DURATION}
     */
    public static String returnString(
        @Nullable String key,
        @NonNull StringSupplierSimpleLogger stringSupplierLogger,
        @NonNull Consumer<StringSupplierSimpleLogger> job) {
        return returnString(
            key,
            stringSupplierLogger,
            DEFAULT_DURATION,
            job
        );
    }

    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)), with no key (meaning that jobs can be started concurrently.
     */
    public static String returnString(
        @NonNull StringSupplierSimpleLogger stringSupplierLogger,
        @NonNull Duration wait,
        @NonNull Consumer<StringSupplierSimpleLogger> job) {
        return returnString(null, stringSupplierLogger, wait, job);
    }

    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)), with no key (meaning that jobs can be started concurrently.
     */
    public static String returnString(
        @NonNull StringSupplierSimpleLogger stringSupplierLogger,
        @NonNull Consumer<StringSupplierSimpleLogger> job) {
        return returnString(null, stringSupplierLogger, DEFAULT_DURATION, job);
    }

    /**
     * A convenience method for {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)} with a multiline logger.
     * @param log A {@link Logger} instance to log to too.
     * @param job A job to run. It is a consumer of a {@link SimpleLogger}, to which it can log its progress.
     * @return
     */
    public static String returnMultilineString(
        @NonNull Logger log,
        @NonNull Consumer<StringSupplierSimpleLogger> job) {
        return returnString(multiLine(log), job);
    }


    /**
     * @deprecated use {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)} instead.
     */
    @Deprecated
    public static String returnString(
        @Nullable final String key,
        @NonNull final StringSupplierSimpleLogger stringSupplierLogger,
        @NonNull final  Duration wait,
        @NonNull final Callable<String> job) {
        return returnString(key, stringSupplierLogger, wait, l -> {
            try {
                String s = job.call();
                l.info(s);
            } catch (Exception e) {
                stringSupplierLogger.error(e.getClass().getName() + " " + e.getMessage(), e);
            }
        }
        );
      }

    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Callable)), with no key (meaning that jobs can be started concurrently.
     * @deprecated use {@link #returnString(StringSupplierSimpleLogger, Duration, Consumer)} instead.
     */
    @Deprecated
    public static String returnString(
        @NonNull StringSupplierSimpleLogger description,
        @NonNull Duration wait,
        @NonNull Callable<String> job) {
        return returnString(null, description, wait, job);
    }


    /**
     * @deprecated use {@link #returnString(StringSupplierSimpleLogger, Duration, Consumer)}
     */
    @Deprecated
    public static String returnString(
        @NonNull Supplier<String> description,
        @NonNull Duration wait,
        @NonNull Callable<String> job) {
        return returnString(null, singleLine(log, description.get()), wait, job);
    }
    /**
     * Defaulting version of {@link #returnString(StringSupplierSimpleLogger, Duration, Callable)} waiting for 5 seconds before time out.
     * @deprecated use {@link #returnString(StringSupplierSimpleLogger, Consumer)}
     */
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
        return multiLine(Level.INFO, log, message, args);
    }

    public static StringSupplierSimpleLogger multiLine(Level level, Logger log, String message, Object... args) {

        StringSupplierSimpleLogger string  = StringBuilderSimpleLogger.builder()
            .prefix((l) -> l.compareTo(Level.ERROR) <= 0 ? "" : l.name() + " ")
            .level(level)
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
     * A String supplier of one line. This can be used as argument for {@link #returnString(String, StringSupplierSimpleLogger, Duration, ThrowAnyConsumer)}
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
     *  A 'visualvm' oriented check whether a string is 'empty'. i.e. not filled.
     * <p>
     *  The point being that also the string 'String' is considered empty, because that is the default value in visualvm, so that's the value when untouched.
     * @since 2.7
     */
    public static boolean isEmpty(String string) {
        return "String".equals(string) || StringUtils.isEmpty(string);
    }

    /**
     * A 'visualvm' oriented check whether a string is 'blank'. i.e. not filled, or only filled with space.
     * <p>
     * The point being that also the string 'String' is considered blank, because that is the default value in visualvm, so that's the value when untouched.
     * @since 2.7
     */
    public static boolean isBlank(String string) {
        return "String".equals(string) || StringUtils.isBlank(string);
    }

    /**
     * @since 5.4
     */
    public static String asString(String string, String defaultValue) {
        return isBlank(string) ? defaultValue : string;
    }

    /**
     * @since 4.0
     */
    public static boolean asBoolean(String string, boolean defaultValue) {
        return isBlank(string) ? defaultValue : Boolean.parseBoolean(string);
    }


    /**
     * @since 2.10
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> void registerBean(ObjectName name, T object) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            unregister(name);
            final String className = object.getClass().getName();
            final String mbeanName = className + "MBean";
            try {
                Class<T> mxBean = (Class<T>) Class.forName(mbeanName);
                mbs.registerMBean( new AnnotatedStandardMXBean(object, mxBean), name);
                return;
            } catch (ClassNotFoundException classNotFoundException) {
                log.info("Interface {} not found: {} (vpro.jmx annotations not supported)", mbeanName, classNotFoundException.getMessage());
            }
            mbs.registerMBean(object, name);
        } catch (NotCompliantMBeanException | MBeanRegistrationException | InstanceAlreadyExistsException e) {
            log.error(e.getMessage(), e);
        } catch (SecurityException se) {
            log.info("For {}: {}", name, se.getMessage());
        }
    }

    /**
     *
     */
    @SneakyThrows
    public static synchronized ObjectName registerBean(Object object, String name) {
        name = name.replaceAll("[^a-zA-Z\\-0-9]+", "_");
        ObjectName objectName = getObjectNameWithName(object, name);
        registerBean(objectName, object);
        return objectName;
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
     * Created a new object name.
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

    @SneakyThrows
    public static ObjectName getObjectNameWithName(Object object, String name) {
        Class<?> clazz= object.getClass();
        return new ObjectName(clazz.getPackage().getName() + ":name=" + name + ",type=" + clazz.getSimpleName());
    }


    @Getter
    @Setter
    public static class LockValue {
        Future<?> future;
        Thread thread;

        @NonNull
        Supplier<String> description;

        private LockValue(@NonNull Supplier<String> description) {
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

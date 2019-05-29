package nl.vpro.jmx;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import nl.vpro.logging.Slf4jHelper;
import nl.vpro.logging.simple.Slf4jSimpleLogger;
import nl.vpro.logging.simple.StringBuilderSimpleLogger;
import nl.vpro.logging.simple.StringSupplierSimpleLogger;

/**
 * Utilities to start jmx tasks in the background.
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class MBeans {

    private static final Map<String, Future> locks = new ConcurrentHashMap<>();

    public static final Duration DEFAULT_DURATION =  Duration.ofSeconds(5);


    public static boolean isRunning(final String key) {
        return locks.containsKey(key);
    }

    public static String cancel(final String key){
        Future future = locks.get(key);
        if (future == null) {
            return "Not running";
        }

        try {
            future.cancel(true);
        } finally {
            locks.remove(key);
        }
        return "Cancelled";


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
        @Nonnull final StringSupplierSimpleLogger description,
        @Nonnull final Duration wait,
        @Nonnull final Consumer<StringSupplierSimpleLogger> logger) {
        if (key != null) {
            if (isRunning(key)) {
                return "Job " + key + " is still running, so could not be started again with " + description.get();
            }
        }
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            final String threadName = Thread.currentThread().getName();
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(MBeans.class.getClassLoader());
                Thread.currentThread().setName(threadName + ":" + description.get());
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
        if (key != null) {
            locks.put(key, future);
        }
        try {
            return future.get(wait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
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
        @Nonnull StringSupplierSimpleLogger description,
        @Nonnull Duration wait,
        @Nonnull Consumer<StringSupplierSimpleLogger> logger) {
        return returnString(null, description, wait, logger);
    }

    /**
     * Defaulting version of {@link #returnString(String, StringSupplierSimpleLogger, Duration, Consumer)), with no key (meaning that jobs can be started concurrently.
     */
    public static String returnString(
        @Nonnull StringSupplierSimpleLogger description,
        @Nonnull Consumer<StringSupplierSimpleLogger> logger) {
        return returnString(null, description, DEFAULT_DURATION, logger);
    }

    public static String returnMultilineString(
        @Nonnull Logger log,
        @Nonnull Consumer<StringSupplierSimpleLogger> logger) {
        return returnString(multiLine(log), logger);
    }



    @Deprecated
    public static String returnString(
        @Nullable final String key,
        @Nonnull final StringSupplierSimpleLogger description,
        @Nonnull final  Duration wait,
        @Nonnull final Callable<String> job) {
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
        @Nonnull StringSupplierSimpleLogger description,
        @Nonnull Duration wait,
        @Nonnull Callable<String> job) {
        return returnString(null, description, wait, job);
    }


    @Deprecated
    public static String returnString(
        @Nonnull Supplier<String> description,
        @Nonnull Duration wait,
        @Nonnull Callable<String> job) {
        return returnString(null, singleLine(log, description.get()), wait, job);
    }
    /**
     * Defaulting version of {@link #returnString(StringSupplierSimpleLogger, Duration, Callable)} waiting for 5 seconds before time out.*/
    @Deprecated
    public static String returnString(
        @Nonnull StringSupplierSimpleLogger description,
        @Nonnull Callable<String> job) {
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
        private CharSequence string;
        private Logger logger;

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


    public boolean isEmpty(String string) {
        return "String".equals(string) || StringUtils.isEmpty(string);
    }


    public boolean isBlank(String string) {
        return "String".equals(string) || StringUtils.isBlank(string);
    }
}

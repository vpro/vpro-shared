package nl.vpro.jmx;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import nl.vpro.logging.Slf4jHelper;
import nl.vpro.logging.simple.SimpleLogger;
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
     * @param description A supplier that before the job starts should describe the job.
     * @param wait How long to wait before returing with a message that the job is not yet finished, but still running.
     * @param job A job returning a String when ready. This string will be returned.
     * @return
     */
    public static String returnString(
        @Nullable final String key,
        @Nonnull final Supplier<String> description,
        @Nonnull final  Duration wait,
        @Nonnull final  Callable<String> job) {
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
                return job.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (key != null) {
                    locks.remove(key);
                }
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                Thread.currentThread().setName(threadName);
            }

        });
        if (key != null) {
            locks.put(key, future);
        }
        try {
            return future.get(wait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            return description + " " + e.getMessage();
        } catch (TimeoutException e) {
            return description.get() + "\n...\nstill busy. Please check logs";
        }
    }


    /**
     * Defaulting version of {@link #returnString(String, Supplier, Duration, Callable)), with no key (meaning that jobs can be started concurrently.
     */
    public static String returnString(
        @Nonnull Supplier<String> description,
        @Nonnull Duration wait,
        @Nonnull Callable<String> job) {
        return returnString(null, description, wait, job);
    }

    /**
     * Defaulting version of {@link #returnString(Supplier, Duration, Callable)} waiting for 5 seconds before time out.*/
    public static String returnString(
        @Nonnull Supplier<String> description,
        @Nonnull Callable<String> job) {
        return returnString(description, Duration.ofSeconds(5), job);
    }


    public static UpdatableString singleLine(Logger log, String message, Object... args) {
        return new UpdatableString(log, message, args);
    }


    public static StringSupplierSimpleLogger multiLine(Logger log, String message, Object... args) {

        StringSupplierSimpleLogger string  = StringBuilderSimpleLogger.builder()
            .prefix((l) -> "")
            .chain(Slf4jSimpleLogger.of(log));
        string.info(message, args);
        return string;
    }

    /**
     * A String supplier of one line. This can be used as argument for {@link #returnString(String, Supplier, Duration, Callable)}
     */
    public static class UpdatableString implements Supplier<String>, SimpleLogger {
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
}

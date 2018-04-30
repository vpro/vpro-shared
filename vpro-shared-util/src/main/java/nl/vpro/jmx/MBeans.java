package nl.vpro.jmx;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import nl.vpro.logging.Slf4jHelper;
import nl.vpro.logging.simple.SimpleLogger;

/**
 * Utilities to start jmx tasks in the background.
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class MBeans {

    static final Map<String, Future> locks = new ConcurrentHashMap<>();

    public static String returnString(Supplier<String> description, Duration wait, Callable<String> job) {
        return returnString(null, description, wait, job);
    }

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

    public static String returnString(final String key, Supplier<String> description, Duration wait, Callable<String> job) {
        if (key != null) {
            if (isRunning(key)) {
                return "Job " + key + " is still running, so could not be started again with " + description.get();
            }
        }
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            final String threadName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName(threadName + ":" + description.get());
                return job.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (key != null) {
                    locks.remove(key);
                }
                Thread.currentThread().setName(threadName);
            }

        });
        if (key != null) {
            locks.put(key, future);
        }
        try {
            return future.get(wait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            return e.getMessage();
        } catch (TimeoutException e) {
            return description  + " still busy. Please check logs";
        }
    }



    public static class UpdatableString implements Supplier<String>, SimpleLogger<UpdatableString> {
        private String string;
        private Logger logger;

        public UpdatableString(Logger logger, String string, Object... args) {
            this.logger = logger;
            info(string, args);
        }

        @Override
        public String get() {
            return string;
        }

        @Override
        public void accept(Level level, String s, Throwable t) {
            if (logger != null) {
                String prev = this.string;
                this.string = s;
                if (Objects.equals(prev, this.string)) {
                    Slf4jHelper.log(logger, level, this.string);
                }
            }
        }

        @Override
        public String toString() {
            return string;
        }
    }
}

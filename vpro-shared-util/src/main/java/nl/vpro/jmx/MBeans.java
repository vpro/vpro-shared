package nl.vpro.jmx;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;

import nl.vpro.util.ThreadPools;

/**
 * Utilities to start jmx tasks in the background.
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class MBeans {


    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(
        ThreadPools.createThreadFactory("MBeans", true, Thread.NORM_PRIORITY));


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
                return "Job " + key + "is still running, so could not be started again with " + description.get();
            }
        }
        Future<String> future = EXECUTOR_SERVICE.submit(() -> {
            final String threadName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName(threadName + ":" + description.get());
                return job.call();
            } finally {
                locks.remove(key);
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



    public static class UpdatableString implements Supplier<String> {
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
        private void set(String s, Object... args) {
            this.string = String.format(s, args);
        }

        private void log(Consumer<String> log, String s, Object... args) {
            if (logger != null) {
                String prev = this.string;
                set(s, args);
                if (Objects.equals(prev, this.string)) {
                    log.accept(this.string);
                }
            }
        }

        public String debug(String s, Object... args) {
            log((string) -> logger.debug(string), s, args);
            return this.string;
        }


        public String info(String s, Object... args) {
            log((string) -> logger.info(string), s, args);
            return this.string;
        }

        public String error(String s, Object... args) {
            log((string) -> logger.error(string), s, args);
            return this.string;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}

package nl.vpro.jmx;

import java.time.Duration;
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
public class MBeans {


    static final ExecutorService executorService = Executors.newCachedThreadPool(
        ThreadPools.createThreadFactory("MBeans", true, Thread.NORM_PRIORITY));



    static public String returnString(Supplier<String> description, Duration wait, Callable<String> job) {

        Future<String> future = executorService.submit(job);
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

package nl.vpro.jmx;

import lombok.extern.log4j.Log4j2;

import java.time.Duration;

import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.ThrowAnyConsumer;
import org.meeuw.functional.ThrowAnyRunnable;

import nl.vpro.logging.log4j2.CaptureToSimpleLogger;
import nl.vpro.logging.simple.*;

/**
 * See {@link MBeans} but supporting log4j2.
 * @author Michiel Meeuwissen
 * @since 2.12
 */
@Log4j2
public class MBeans2 {

    /**
     * Returns a piece of code in the background. The logging of this job is returned, unless it took to long (> {@link MBeans#DEFAULT_DURATION}) in which case only the part up until then is returned.
     *
     * @param log The (logj2) logger to provide to the code.
     * @param job The code the run. As a consumer of a {@link SimpleLogger} to which it can log.
     * @return (The first part) of the logging of the job.
     * @see MBeans#returnMultilineString(org.slf4j.Logger, java.util.function.Consumer)
     */
     public static String returnMultilineString(
        @NonNull Logger log,
        @NonNull ThrowAnyConsumer<StringSupplierSimpleLogger> job) {
        return MBeans.returnString(
            multiLine(log), job
        );
    }


    /**
     * Run a job (probably for JMX) capturing (log4j2) logging into a multiline string.
     * @param job The job to run.
     * @param currentThreadOnly If true, only logging from the current thread, and the thread the job will be logging in will be captured
     * @return The (first lines) of the result of the logging of the job.
     * @since 5.13
     */
    public static String returnMultilineString(
        String key,
        Duration timeout,
        @NonNull ThrowAnyRunnable job,
        boolean currentThreadOnly) {

        StringSupplierSimpleLogger logger = StringBuilderSimpleLogger.builder()
            .prefix(l -> l.compareTo(Level.WARN) > 0 ? "" : l.name())
            .build();
        try (CaptureToSimpleLogger capture = CaptureToSimpleLogger.of(logger, currentThreadOnly)) {
            return MBeans.returnString(
                key,
                logger,
                timeout,
                (logging) -> {
                    try (var dis = capture.associateWithCurrentThread()) {
                        job.run();
                    }
                }
            );
        }
    }

    /**
     * Defaults to {@code currentThreadOnly = false}
     * @param job The runnable to run
     * @see #returnMultilineString(String, Duration, ThrowAnyRunnable, boolean)
     * @since 5.13
     */
    public static String returnMultilineString(
        @NonNull ThrowAnyRunnable job) {
        return returnMultilineString(null, MBeans.DEFAULT_DURATION, job, false);
    }


    /**
     * @param log Logger instance to log too
     * @return a {@link StringBuilderSimpleLogger} representing multiple lines. Actually a {@link StringBuilderSimpleLogger}
     */
    public static StringSupplierSimpleLogger multiLine(Logger log) {
        return multiLine(log, null);
    }

     /**
     * @param log Logger instance to log too
     * @param initialMessage First line of the string (logged as info)
     * @param args The arguments of the first line
     * @return a {@link StringBuilderSimpleLogger} representing multiple lines actually a {@link StringBuilderSimpleLogger}
     */
    public static StringSupplierSimpleLogger multiLine(
        Logger log,
        @Nullable String initialMessage,
        Object... args) {
        StringSupplierSimpleLogger string  = StringBuilderSimpleLogger.builder()
            .prefix(l -> l.compareTo(Level.WARN) > 0 ? "" : l.name())
            .chain(Log4j2SimpleLogger.of(log));
        if (initialMessage != null) {
            string.info(initialMessage, args);
        }
        return string;
    }

}

package nl.vpro.util;

import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utilities related to ThreadPools
 * Perhaps this can be deprecated in favour of {@link  ForkJoinPool#commonPool()}?
 * @author Michiel Meeuwissen
 * @since 1.5
 */
@Log
public final class ThreadPools {

    private ThreadPools() {
        // this class has no instances
    }

    public static final ThreadGroup THREAD_GROUP = new ThreadGroup(ThreadPools.class.getName());


    public static ThreadFactory createThreadFactory(final String namePrefix, final boolean daemon, final int priority) {
        return createThreadFactory(THREAD_GROUP, namePrefix, daemon, priority);
    }
    public static ThreadFactory createThreadFactory(final ThreadGroup threadGroup, final String namePrefix, final boolean daemon, final int priority) {
        //return Thread.ofVirtual().factory();
        return new ThreadFactory() {
            long counter = 1;

            @Override
            public Thread newThread(@NonNull Runnable r) {

                Thread thread = new Thread(threadGroup, r);
                thread.setContextClassLoader(ThreadPools.class.getClassLoader());
                thread.setDaemon(daemon);
                thread.setPriority(priority);
                thread.setName(namePrefix +
                    (namePrefix.endsWith("-") ? "" : "-") +
                    (counter++)
                );
                return thread;
            }
        };
    }

    /**
     * An executor service used for 'copy' threads. Mainly in {@link Copier}, but it can be used for similar processes.
     * <p>
     * These may be quite long-lived thread, performing simple jobs like copying streams.
     */
    public static final ExecutorService copyExecutor = createExecutor(() -> {
        return new ThreadPoolExecutor(2, 2000, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            createThreadFactory(
                "nl.vpro.util.threadpools-Copier",
                false,
                Thread.NORM_PRIORITY));
    });

    public static String copyExecutorDescription() {
        if (copyExecutor instanceof ThreadPoolExecutor tpe) {
            return String.format("copyExecutor: %s, pool size: %d, active threads: %d, queue size: %d",
                copyExecutor.getClass().getSimpleName(),
                tpe.getPoolSize(), tpe.getActiveCount(), tpe.getQueue().size());
        } else {
            return "copyExecutor: " + copyExecutor.getClass().getSimpleName();
        }

    }

    /**
     * Creates a virtual thread by task executor if on java >=21, otherwise create using the fallback.
     * @param fallback
     * @return
     */
    public static ExecutorService  createExecutor(Supplier<ExecutorService> fallback) {
        return createExecutor("newVirtualThreadPerTaskExecutor", fallback);
    }
    @SneakyThrows
    public static <E extends ExecutorService> E createExecutor(String virtual, Supplier<E> fallback) {
        try {
            // Try to use virtual threads if available (Java 21+)
            var method = Executors.class.getMethod(virtual);
            log.fine("Using virtual threads for copy executor");
            return (E) method.invoke(null);
        } catch (NoSuchMethodException e) {
            E fallbackExecutor = fallback.get();
            log.info("Virtual threads not available (requires Java 21+), using " + fallbackExecutor);
            return fallbackExecutor;

        }
    }



    /**
     * An executor service used for relatively long-lived background jobs.
     * <p>
     * These may be quite long-lived thread, performing more complex jobs like complicated SQL queries.
     * @since 3.0
     */
    public static final ExecutorService longBackgroundExecutor = createExecutor(() ->
        new ThreadPoolExecutor(2, 100, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            createThreadFactory(
                "nl.vpro.util.threadpools-LongBackground",
                false,
                Thread.NORM_PRIORITY))
    );

    /**
     * A scheduled executor service with <em>fixed pool size</em>, so should be used to schedule short-lived background tasks only.
     */

    public static final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(5,
            createThreadFactory(
                "nl.vpro.util.threadpools-Background",
                true,
                Thread.MIN_PRIORITY)
    );


    /**
     * An executor service used for threads running during bootstrap of the application. Core size is 0, so that after a few minutes (when all is up) all threads will be shut down.
     */
    public static final ThreadPoolExecutor startUpExecutor =
        new ThreadPoolExecutor(0, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(),
            createThreadFactory(
                "nl.vpro-util-StartUp",
                false,
                Thread.MAX_PRIORITY));

    public static void shutdown() {
        log.fine("Shutting down thread pools");
        copyExecutor.shutdown();
        startUpExecutor.shutdown();
        backgroundExecutor.shutdown();
        longBackgroundExecutor.shutdown();
    }

    public static void shutdownNowAndWait(String description, ExecutorService  executor) {
        shutdownNowAndWait(description, executor, Duration.ofSeconds(30));
    }

    public static void shutdownNowAndWait(String description, ExecutorService  executor, Duration duration) {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warning("%s executor did not terminate within %s ".formatted(description, duration));
            } else {
                log.info("%s executor terminated cleanly".formatted(description));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warning("Interrupted while waiting for %s executor to terminate".formatted(description));
        }
    }
}


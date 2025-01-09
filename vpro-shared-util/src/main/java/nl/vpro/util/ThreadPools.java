package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utilities related to ThreadPools
 * Perhaps this can be deprecated in favour of {@link  ForkJoinPool#commonPool()}?
 * @author Michiel Meeuwissen
 * @since 1.5
 */
@Slf4j
public final class ThreadPools {

    private ThreadPools() {
        // this class has no instances
    }

    public static final ThreadGroup THREAD_GROUP = new ThreadGroup(ThreadPools.class.getName());



    public static ThreadFactory createThreadFactory(final String namePrefix, final boolean daemon, final int priority) {
        return createThreadFactory(THREAD_GROUP, namePrefix, daemon, priority);
    }
    public static ThreadFactory createThreadFactory(final ThreadGroup threadGroup, final String namePrefix, final boolean daemon, final int priority) {
        return new ThreadFactory() {
            long counter = 1;

            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(threadGroup, r);
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
    public static final ThreadPoolExecutor copyExecutor =
        new ThreadPoolExecutor(2, 2000, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            ThreadPools.createThreadFactory(
                "nl.vpro.util.threadpools-Copier",
                false,
                Thread.NORM_PRIORITY));


    /**
     * An executor service used for relatively long-lived background jobs.
     * <p>
     * These may be quite long-lived thread, performing more complex jobs like complicated SQL queries.
     * @since 3.0
     */
    public static final ThreadPoolExecutor longBackgroundExecutor =
        new ThreadPoolExecutor(2, 100, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            ThreadPools.createThreadFactory(
                "nl.vpro.util.threadpools-LongBackground",
                false,
                Thread.NORM_PRIORITY));

    /**
     * A scheduled executor service with <em>fixed pool size</em>, so should be used to schedule short-lived background tasks only.
     */
    public static final ScheduledExecutorService backgroundExecutor =
        Executors.newScheduledThreadPool(5,
            ThreadPools.createThreadFactory(
                "nl.vpro.util.threadpools-Background",
                true,
                Thread.MIN_PRIORITY));


    /**
     * An executor service used for threads running during bootstrap of the application. Core size is 0, so that after a few minutes (when all is up) all threads will be shut down.
     */
    public static final ThreadPoolExecutor startUpExecutor =
        new ThreadPoolExecutor(0, 20, 60, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(),
            ThreadPools.createThreadFactory(
                "nl.vpro-util-StartUp",
                false,
                Thread.NORM_PRIORITY));

    public static void shutdown() {
        log.debug("Shutting down thread pools");
        copyExecutor.shutdown();
        startUpExecutor.shutdown();
        backgroundExecutor.shutdown();
        longBackgroundExecutor.shutdown();
    }
}


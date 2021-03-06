package nl.vpro.util;

import java.util.concurrent.*;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utilities related to ThreadPools
 * Perhaps this can be deprecated in favour of {@link  ForkJoinPool#commonPool()}?
 * @author Michiel Meeuwissen
 * @since 1.5
 */
public final class ThreadPools {

    private ThreadPools() {
        // this class has no intances
    }

    private static final ThreadGroup THREAD_GROUP = new ThreadGroup(ThreadPools.class.getName());


    public static ThreadFactory createThreadFactory(final String namePrefix, final boolean daemon, final int priority) {
        return new ThreadFactory() {
            long counter = 1;

            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(THREAD_GROUP, r);
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
     */
    public static final ThreadPoolExecutor copyExecutor =
        new ThreadPoolExecutor(0, 2000, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            ThreadPools.createThreadFactory(
                "nl.vpro-util-Copier",
                false,
                Thread.NORM_PRIORITY));

    /**
     * A scheduled executor service with _fixed pool size_, so should be used to schedule short lived background tasks only.
     */
    public static final ScheduledExecutorService backgroundExecutor =
        Executors.newScheduledThreadPool(5,
            ThreadPools.createThreadFactory(
                "nl.vpro-util-Background",
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
        copyExecutor.shutdown();
        startUpExecutor.shutdown();
        backgroundExecutor.shutdown();
	}
}


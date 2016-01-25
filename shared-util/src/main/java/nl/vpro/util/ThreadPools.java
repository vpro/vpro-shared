package nl.vpro.util;

import java.util.concurrent.*;

/**
 * Utilities related to ThreadPools
 * @author Michiel Meeuwissen
 * @since 1.5
 */
public final class ThreadPools {

    private ThreadPools() {
        // this class has no intances
    }

    private static ThreadGroup THREAD_GROUP = new ThreadGroup(ThreadPools.class.getName());


    public static ThreadFactory createThreadFactory(final String namePrefix, final boolean daemon, final int priority) {
        return new ThreadFactory() {
            long counter = 1;
            @Override
            public Thread newThread(Runnable r) {
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

    public static final ThreadPoolExecutor copyExecutor =
        new ThreadPoolExecutor(0, 2000, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            ThreadPools.createThreadFactory(
                "nl.vpro-util-Copier",
                false,
                Thread.NORM_PRIORITY));

    public static final ScheduledExecutorService backgroundExecutor =
        Executors.newScheduledThreadPool(1,
            ThreadPools.createThreadFactory(
                "nl.vpro-util-Background",
                false,
                Thread.MIN_PRIORITY));

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
	}
}


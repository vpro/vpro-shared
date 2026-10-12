package nl.vpro.util;

import lombok.extern.java.Log;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * A junit TestExecutionListener that just makes sure that all ThreadPools are shutdown after the tests.
 * Also, it calls {@link ForkJoinPool#awaitQuiescence(long, TimeUnit)} (100s) on {@link ForkJoinPool#commonPool()}
 */
@Log
public class ThreadPoolShutdowner implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        if (ThreadPoolsState.INITIALIZED) {
            ThreadPools.shutdown();
        } else {
            log.fine("ThreadPools was never used, no need to shut it down");
        }
        log.fine("Waiting for ForkJoinPool.commonPool() too");
        // commonPool cannot be shut down. But you can wait for tasks
        boolean b = ForkJoinPool.commonPool().awaitQuiescence(100, TimeUnit.SECONDS);
        if (! b) {
            log.warning("Awaiting common pool took too long");
        }

    }

}

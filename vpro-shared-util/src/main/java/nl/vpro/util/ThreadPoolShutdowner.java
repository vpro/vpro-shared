package nl.vpro.util;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * A junit TestExecutionListener that just makes sure that all ThreadPools are shutdown after the tests.
 * Also, it calls {@link ForkJoinPool#awaitQuiescence(long, TimeUnit)} (100s) on {@link ForkJoinPool#commonPool()}
 */
@Log4j2
public class ThreadPoolShutdowner implements TestExecutionListener {

    @SuppressWarnings("resource")
    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        ThreadPools.shutdown();
        log.debug("Waiting for ForkJoinPool.commonPool() too");
        // commonPool cannot be shut down. But you can wait for tasks
        boolean b = ForkJoinPool.commonPool().awaitQuiescence(100, TimeUnit.SECONDS);
        if (! b) {
            log.info("Awaiting common pool took too long");
        }

    }

}

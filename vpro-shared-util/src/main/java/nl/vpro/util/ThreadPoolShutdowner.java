package nl.vpro.util;

import lombok.extern.log4j.Log4j2;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import java.util.concurrent.ForkJoinPool;

/**
 * A junit TestExecutionListener that just makes sure that all ThreadPools are shutdown after the tests.
 */
@Log4j2
public class ThreadPoolShutdowner implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        ThreadPools.shutdown();
        log.info("Shutting down ForkJoinPool.commonPool() too");
        ForkJoinPool.commonPool().shutdown();
    }

}

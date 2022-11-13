package nl.vpro.util;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ThreadPoolShutdowner implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        ThreadPools.shutdown();
    }

}

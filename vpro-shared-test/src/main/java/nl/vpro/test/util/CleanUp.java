package nl.vpro.test.util;

import lombok.extern.java.Log;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import nl.vpro.test.util.jaxb.JAXBTestUtil;

@Log
public class CleanUp implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        JAXBTestUtil.cleanup();

    }
}

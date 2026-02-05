package nl.vpro.shared.log4j2.test;

import java.io.IOException;
import java.util.logging.LogManager;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;


/**
 * /logging.properties is automaticallly picked up in servlet environment.
 * <p>
 * This makes it work in jupiter tests too.
 *
 * @since 5.15
 */
public class Jul2Log4j2 implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        try {
            LogManager.getLogManager().readConfiguration(getClass().getResourceAsStream("/logging.properties"));//, (s) -> (ss, m) -> {return m;});
        } catch (IOException ignore) {

        }
    }


}

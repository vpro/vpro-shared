package nl.vpro.test.jupiter;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.extension.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Puts information about current test on {@link ThreadContext Mapped Diagnostic Context}.
 * @author Michiel Meeuwissen
 * @since 5.2
 */
public class TestMDC implements AfterTestExecutionCallback, BeforeTestExecutionCallback, BeforeAllCallback,
    AfterAllCallback {

    public static final String KEY        = "currentTest";
    public static final String NUMBER_KEY = "testNumber";
    public static final String ENV        = "env";
    public static  ThreadLocal<Set<String>> TAGS        = ThreadLocal.withInitial(HashSet::new);



    static final protected AtomicInteger testNumber = new AtomicInteger(0);

    @Override
    public void afterTestExecution(ExtensionContext context) {
        ThreadContext.remove(KEY);
        ThreadContext.remove(NUMBER_KEY);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        ThreadContext.put(KEY, context.getRequiredTestClass().getSimpleName() + "#" + context.getRequiredTestMethod().getName());
        ThreadContext.put(NUMBER_KEY, testNumber.incrementAndGet() + ":");
        TAGS.set(context.getTags());
    }

    public static int getTestNumber() {
        return testNumber.get();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ThreadContext.remove(ENV);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {


    }
}

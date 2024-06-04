package nl.vpro.test.jupiter;


import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Together with the interface {@link WithJob}, this allows to do something after a test failed, or any exception was thrown.
 * @author Michiel Meeuwissen
 * @since 5.2
 */
@Log4j2
public class DoAfterException implements InvocationInterceptor {

    public DoAfterException() {
    }
    @Override
    public void interceptTestMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        final Consumer<Throwable> job =
            ((WithJob) extensionContext.getRequiredTestInstance()).getJob();
        try {
            invocation.proceed();
        } catch (Throwable t) {
            if (t instanceof TestAbortedException) {
                log.warn(t.getMessage());
            } else {
                job.accept(t);
            }
            throw t;
        }
    }

    public interface WithJob {
        Consumer<Throwable> getJob();
    }
}

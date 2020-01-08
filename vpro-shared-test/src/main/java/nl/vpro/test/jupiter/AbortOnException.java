package nl.vpro.test.jupiter;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

/**
 * Sometimes, if you run with ordered methods in a test class there is no point in proceeding if an earlier test failed.
 * This extension will arrange that all further tests in the class are ignored if one fails.
 *
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@Slf4j
public class AbortOnException extends ExceptionCollector implements InvocationInterceptor, BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        //store = context.getStore(Namespace.create(context.getRequiredTestClass()));
        TestMethodOrder annotation = context.getRequiredTestClass().getAnnotation(TestMethodOrder.class);
        if (annotation == null || annotation.value().equals(MethodOrderer.Random.class)) {
            throw new IllegalStateException("The abort on exception extension only makes sense with ordered test methods");
        }

    }
    @Override
    public void interceptTestMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        boolean hasNoAbortAnnotation =  invocationContext.getExecutable().getAnnotation(NoAbort.class) != null;
        boolean skip = ! hasNoAbortAnnotation && ! fails.isEmpty();
        if (skip) {
             throw new TestAbortedException("An exception occured already " + fails.get(0).getInvocationContext().getExecutable().getName());
         }
        super.interceptTestMethod(invocation, invocationContext, extensionContext);
    }



}

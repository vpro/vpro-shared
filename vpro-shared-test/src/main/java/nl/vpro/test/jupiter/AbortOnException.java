package nl.vpro.test.jupiter;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.*;
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

    protected boolean active = true;

    @Override
    public void beforeAll(ExtensionContext context) {
        //store = context.getStore(Namespace.create(context.getRequiredTestClass()));
        TestMethodOrder annotation = context.getRequiredTestClass().getAnnotation(TestMethodOrder.class);
        OnlyIfOrdered onlyIfOrdered = context.getRequiredTestClass().getAnnotation(OnlyIfOrdered.class);

        if (annotation == null || annotation.value().equals(MethodOrderer.Random.class)) {
            if (onlyIfOrdered == null) {
                throw new IllegalStateException("The abort on exception extension only makes sense with ordered test methods");
            } else {
                log.info("Tests are not ordered. Disabled AbortOnException extension");
                active = false;
            }
        }

    }
    @Override
    public void interceptTestMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {

        if (active) {
            boolean hasNoAbortAnnotation = invocationContext.getExecutable().getAnnotation(NoAbort.class) != null;
            boolean skip = !hasNoAbortAnnotation && !fails.isEmpty();
            if (skip) {
                throw new TestAbortedException("An exception occured already " + fails.get(0).getInvocationContext().getExecutable().getName());
            }
        }
        super.interceptTestMethod(invocation, invocationContext, extensionContext);
    }


    /**
     * In combination with {@link AbortOnException}. A test annotated with this will not be skipped, even if a previous test failed.
     *
     * Sometimes in integrations test some later test mainly perform clean up actions, and you'd better not skip it.
     * @since 2.10
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface NoAbort {
    }
    /**
     * @since 2.10
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    public @interface OnlyIfOrdered {
    }
}

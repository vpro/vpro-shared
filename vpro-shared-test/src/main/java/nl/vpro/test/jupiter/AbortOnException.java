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
    protected int relevantFails = 0;

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

            boolean skip = !hasNoAbortAnnotation && relevantFails > 0;
            if (skip) {
                throw new TestAbortedException("An exception occured already " + fails.get(0).getInvocationContext().getExecutable().getName());
            }
        }
        boolean hasExcept = invocationContext.getExecutable().getAnnotation(Except.class) != null;
        int sizeOfFails = fails.size();
        try {
            super.interceptTestMethod(invocation, invocationContext, extensionContext);
        } finally {
            if (!hasExcept) {
                relevantFails += (fails.size() - sizeOfFails);
            }
        }

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
        String value() default "";
    }

     /**
      * In combination with {@link AbortOnException}. A test annotated with this won't abort the rest of the tests even if it fails
     *
     * @since 2.12
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Except {
        String value() default "";
    }

    /**
     * Only it the test class is also 'ordered' apply the abort on exception logic.
     *
     * You may place the {@link AbortOnException} on super class, where some sub classes are not ordered, and for those it will not apply then.
     * @since 2.10
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Inherited
    public @interface OnlyIfOrdered {
    }
}

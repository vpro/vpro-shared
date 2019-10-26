package nl.vpro.test.jupiter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.opentest4j.TestAbortedException;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class AbortOnException implements InvocationInterceptor, BeforeAllCallback {

    ExtensionContext.Store store;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        store = context.getStore(Namespace.create(context.getRequiredTestClass()));
        TestMethodOrder annotation = context.getRequiredTestClass().getAnnotation(TestMethodOrder.class);
        if (annotation == null || annotation.value().equals(MethodOrderer.Random.class)) {
            throw new IllegalStateException("The abort on exception extnesion only makes sense with ordered test methods");
        }

    }
    @Override
    public void interceptTestMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {

         Fail fail = (Fail) store.get("exception");
         if (fail != null){
             throw new TestAbortedException("An exception occured already " + fail.getInvocationContext().getExecutable().getName());
         }
         try {
             invocation.proceed();
         } catch (Exception e) {
             store.put("exception", new Fail(invocation, invocationContext, e));
             throw e;
         }
    }

    @Getter
    class Fail {
        private final Throwable exception;
        private final ReflectiveInvocationContext<Method>  invocationContext;
        private final Invocation<Void>  invocation;


        Fail(
            Invocation<Void>  invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            Throwable exception) {
            this.invocation = invocation;
            this.invocationContext = invocationContext;
            this.exception = exception;

        }
    }

}

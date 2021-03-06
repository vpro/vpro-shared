package nl.vpro.test.jupiter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.*;
import org.opentest4j.IncompleteExecutionException;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@Slf4j
public class ExceptionCollector implements InvocationInterceptor, ParameterResolver {

    List<Fail> fails = new ArrayList<>();

    @Override
    public void interceptTestMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {


         try {
             invocation.proceed();
         } catch (IncompleteExecutionException  te) {
             log.debug("These kind of exceptions are used by junit itself to abort tests, and are not exception which should kind as failures.");
             throw te;
         } catch (Throwable e) {
             fails.add(new Fail(invocation, invocationContext, e));
             throw e;
         }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        boolean collection = parameterContext.getParameter().getType().isAssignableFrom(List.class);
        if (collection) {
            ParameterizedType type = (ParameterizedType) parameterContext.getParameter().getParameterizedType();
            return Throwable.class.isAssignableFrom((Class) type.getActualTypeArguments()[0]);
        }  else {
            return false;
        }

    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return fails.stream().map(f -> f.exception).collect(Collectors.toList());

    }

    @Getter
    static class Fail {
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

package nl.vpro.test.jupiter;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@Slf4j
public class ParameterizedClass implements  InvocationInterceptor, BeforeTestExecutionCallback, ParameterResolver {



    @Override
    public void interceptBeforeEachMethod(InvocationInterceptor.Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        log.info("Intercepting before {}", invocationContext);
        invocation.proceed();
	}

    @Override
    public <T> T interceptTestClassConstructor(Invocation<T> invocation,
                                               ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext)
			throws Throwable {
		return invocation.proceed();
	}
	@Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
        log.info("Intercepting {}", invocationContext);
		invocation.proceed();
	}



    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return false;

    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return null;

    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        log.info("--");

    }
}

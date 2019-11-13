package nl.vpro.test.jupiter;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.extension.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class Browsers implements ParameterResolver, TestInstancePostProcessor {
    @Override
    public boolean supportsParameter(
        ParameterContext parameterContext,
        ExtensionContext extensionContext) throws ParameterResolutionException {
        return false;

    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return 1;

    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        log.info("Processing {}", testInstance);

    }
}

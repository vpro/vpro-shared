package nl.vpro.test.jupiter;

import org.junit.jupiter.api.extension.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class Browsers implements ParameterResolver {
    @Override
    public boolean supportsParameter(
        ParameterContext parameterContext,
        ExtensionContext extensionContext) throws ParameterResolutionException {
        return true;

    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return 1;

    }
}

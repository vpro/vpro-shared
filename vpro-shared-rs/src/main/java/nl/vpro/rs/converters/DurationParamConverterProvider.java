package nl.vpro.rs.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Duration;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 0.28
 */
@Provider
public class DurationParamConverterProvider implements ParamConverterProvider {
    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Duration.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) DurationParamConverter.INSTANCE;
        }

        return null;

    }
}

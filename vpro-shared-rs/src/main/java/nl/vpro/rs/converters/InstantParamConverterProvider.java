package nl.vpro.rs.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 1.73
 */
@Provider
public class InstantParamConverterProvider implements ParamConverterProvider {
    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Instant.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) InstantParamConverter.INSTANCE;
        }
        return null;
    }
}

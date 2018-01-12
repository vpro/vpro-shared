package nl.vpro.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 1.73
 */
@Provider
public class InstantParamConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Instant.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) InstantParamConverter.INSTANCE;
        }
        return null;
    }
}

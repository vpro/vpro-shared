package nl.vpro.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Duration;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

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

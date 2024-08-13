package nl.vpro.rs.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Locale;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
@Provider
public class LocaleParamConverterProvider implements ParamConverterProvider {
    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Locale.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) LocaleParamConverter.INSTANCE;
        }

        return null;
    }
}

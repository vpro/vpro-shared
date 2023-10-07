package nl.vpro.rs.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Provider
public class CaseInsensitiveEnumParamConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Enum.class.isAssignableFrom(rawType)) {
            Class<? extends Enum> clazz = (Class<Enum<?>>) rawType;
            return CaseInsensitiveEnumParamConverter.getInstant(clazz);
        } else {
            return null;
        }

    }
}

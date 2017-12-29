package nl.vpro.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

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

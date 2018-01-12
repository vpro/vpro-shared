package nl.vpro.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Locale;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
@Provider
public class LocaleParamConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Locale.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) LocaleParamConverter.INSTANCE;
        }

        return null;

    }
}

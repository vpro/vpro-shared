package nl.vpro.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

/**
 * Makes available {@link DateParamConverter}, {@link LocalDateParamConverter} and {@link InstantParamConverter}
 * @author Michiel Meeuwissen
 * @since 0.23
 */
@Provider
public class DateParamConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Date.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) DateParamConverter.INSTANCE;
        } else if (LocalDate.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) LocalDateParamConverter.INSTANCE;
        } else if (Instant.class.isAssignableFrom(rawType)) {
            return (ParamConverter<T>) InstantParamConverter.INSTANCE;
        }
        return null;

    }
}

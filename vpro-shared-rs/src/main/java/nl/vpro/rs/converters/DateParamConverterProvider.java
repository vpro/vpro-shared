package nl.vpro.rs.converters;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * Makes available {@link DateParamConverter}, {@link LocalDateParamConverter} and {@link InstantParamConverter}
 * @author Michiel Meeuwissen
 * @since 0.23
 */
@Provider
@Slf4j
public class DateParamConverterProvider implements ParamConverterProvider {
    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                log.debug("{}", annotation);
            }
        }
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

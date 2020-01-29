package nl.vpro.rs.converters;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.ws.rs.ext.ParamConverter;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class CaseInsensitiveEnumParamConverter<T extends Enum<T>> implements ParamConverter<T> {


    public static <S extends Enum<S>> CaseInsensitiveEnumParamConverter getInstant(Class<S> enumClass) {
        return new CaseInsensitiveEnumParamConverter<S>(enumClass);
    }

    private final Class<T> enumClass;


    public CaseInsensitiveEnumParamConverter(Class<T> enumClass) {
        this.enumClass = enumClass;
    }



    @Override
    public T fromString(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            try {
                return Enum.valueOf(enumClass, value.toUpperCase());
            } catch (IllegalArgumentException e2) {
                for (Field f : enumClass.getFields()) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        try {
                            T v = (T) f.get(null);
                            if (v.name().equalsIgnoreCase(value)) {
                                return v;
                            }
                        } catch (IllegalAccessException e1) {
                            log.warn(e1.getMessage(), e1);

                        }
                    }
                }
            }
            throw e;
        }
    }

    @Override
    public String toString(T value) {
        if (value == null) {
           return null;
        }
        return value.name();
    }
}

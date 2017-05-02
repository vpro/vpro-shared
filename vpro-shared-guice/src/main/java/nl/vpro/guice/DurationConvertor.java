package nl.vpro.guice;


import java.time.Duration;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeConverter;

import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class DurationConvertor implements TypeConverter {

    public static void register(Binder binder) {
        binder.convertToTypes(Matchers.only(TypeLiteral.get(Duration.class)), new DurationConvertor());
    }
    @Override
    public Object convert(String s, TypeLiteral<?> typeLiteral) {
        if (typeLiteral.getRawType().isAssignableFrom(Duration.class)) {
            return TimeUtils.parseDuration(s).orElse(null);
        }
        return null;

    }
}

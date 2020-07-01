package nl.vpro.guice;


import java.time.Duration;
import java.time.temporal.TemporalAmount;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeConverter;

import nl.vpro.util.TimeUtils;

/**
 * @author Michiel Meeuwissen
 * @since 2.14
 */
public class TemporalAmountConvertor implements TypeConverter {

    public static void register(Binder binder) {
        binder.convertToTypes(Matchers.only(TypeLiteral.get(TemporalAmountConvertor.class)), new TemporalAmountConvertor());
    }
    @Override
    public Object convert(String s, TypeLiteral<?> typeLiteral) {
        Class<?> rawType = typeLiteral.getRawType();
        if (rawType.equals(Duration.class)) {
            // leave to DurationConvertor
            return null;
        }
        if (rawType.isAssignableFrom(TemporalAmount.class)) {
            return TimeUtils.parseTemporalAmount(s).orElse(null);
        }
        return null;
    }
}

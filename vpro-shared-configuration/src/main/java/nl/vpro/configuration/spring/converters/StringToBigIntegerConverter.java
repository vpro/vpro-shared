
package nl.vpro.configuration.spring.converters;

import java.math.BigInteger;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.converter.Converter;

/**
 * Convert String to BigInteger, allowing underscores and surrounding spaces.
 * @author Michiel Meeuwissen
 * @since 5.14.1
 */
public class StringToBigIntegerConverter implements Converter<String, BigInteger> {

    @Override
    public BigInteger convert(@NonNull String text) {
        String cleaned = text.trim().replace("_", "");
        return new BigInteger(cleaned, 10);
    }
}

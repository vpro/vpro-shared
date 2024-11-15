package nl.vpro.configuration.spring.converters;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;

/**

 * @author Michiel Meeuwissen
 * @since 5.4
 */
//@Component
public class StringToMapConverter implements ConditionalConverter, Converter<String, Map<String, String>> {

    public static final StringToMapConverter INSTANCE = new StringToMapConverter();

    @Override
    public Map<String, String> convert(@NonNull String string) {
        if (StringUtils.isBlank(string)) {
            return Map.of();
        }
        return Arrays.stream(string.split("\\s*,\\s*"))
            .map(s -> {
                return s.split("=", 2);
            }).collect(Collectors.toMap((split) -> split[0], (split) -> split[1]));
    }


    @Override
    public boolean matches(
        @NonNull TypeDescriptor sourceType,
        @NonNull TypeDescriptor targetType) {
        if (sourceType.getType().equals(String.class) &&
            targetType.getType().equals(Map.class)) {
            return ((ParameterizedType) targetType.getResolvableType().getType()).getActualTypeArguments()[0].equals(String.class) &&
                ((ParameterizedType) targetType.getResolvableType().getType()).getActualTypeArguments()[1].equals(String.class);
        }
        return false;
    }
}

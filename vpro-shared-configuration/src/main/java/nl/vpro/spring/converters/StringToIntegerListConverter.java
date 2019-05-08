package nl.vpro.spring.converters;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;

/**
 *
 * @author Michiel Meeuwissen
 * @since 1.78
 */
//@Component
public class StringToIntegerListConverter implements ConditionalConverter, Converter<String, List<Integer>> {

    public static final StringToIntegerListConverter INSTANCE = new StringToIntegerListConverter();

    @Override
    public List<Integer> convert(String string) {
        List<Integer> result = new ArrayList<>();
        for (String s : string.split("\\s*,\\s*")) {
            String[] split =  s.split("-", 4);
            if (split.length == 1) {
                result.add(Integer.parseInt(split[0]));
            } else {
                int i = 0;
                String first = split[i++];
                if (first.isEmpty()) {
                    first = "-" + split[i++];
                }
                if (split.length == i) {
                    result.add(Integer.parseInt(first));
                } else {
                    String second = split[i++];
                    if (second.isEmpty()) {
                        second = "-" + split[i];
                    }
                    for (int j = Integer.parseInt(first); j <= Integer.parseInt(second); j++) {
                        result.add(j);
                    }
                }
            }
        }
        return result;

    }

    @Override
    public boolean matches(@Nonnull TypeDescriptor sourceType, @Nonnull TypeDescriptor targetType) {
        if (sourceType.getType().equals(String.class) && targetType.getType().equals(List.class)) {
            if (((ParameterizedType) targetType.getResolvableType().getType()).getActualTypeArguments()[0].equals(Integer.class)) {
                return true;
            }
        }
        return false;

    }
}

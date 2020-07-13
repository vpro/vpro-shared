package nl.vpro.spring.converters;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * Converts a string to  a list of integers.
 *
 * It recognizes these kind of things:
 *<ul>
 *    <li><pre>1,2,3</pre></li>
 *    <li><pre>1..3</pre></li>
 *    <li><pre>1-3, 5, 6</pre></li>
 *</ul>
 * I.e. it recognizes explicitely stated values, ranges (by either a hyphen or by double dots), and combinations of those.
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
            add(s, result::add);
        }
        return result;
    }

    private void add(String s, IntConsumer result) {
        String[] splitByDots = s.split("\\.\\.", 2);
        if (splitByDots.length == 2) {
            addRange(Integer.parseInt(splitByDots[0]), Integer.parseInt(splitByDots[1]), result);
        } else {
            String[] split =  s.split("-", 4); // "-5--4"; -> "", "5", "", "4"
            int i = 0;
            String first = split[i++];
            if (first.isEmpty()) {
                first = "-" + split[i++];
            }
            if (split.length == i) {
                // just one number
                result.accept(Integer.parseInt(first));
            } else {
                // must be a range
                String second = split[i++];
                if (second.isEmpty()) {
                    second = "-" + split[i];
                }
                addRange(Integer.parseInt(first), Integer.parseInt(second), result);
            }
        }
    }
    private void addRange(int start, int end, IntConsumer consumer) {
        for (int j = start; j <= end; j++) {
            consumer.accept(j);
        }
    }

    @Override
    public boolean matches(
        @NonNull TypeDescriptor sourceType,
        @NonNull TypeDescriptor targetType) {
        if (sourceType.getType().equals(String.class) &&
            targetType.getType().equals(List.class)) {
            return ((ParameterizedType) targetType.getResolvableType().getType()).getActualTypeArguments()[0].equals(Integer.class);
        }
        return false;

    }
}

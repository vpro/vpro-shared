package nl.vpro.util;

import java.util.Arrays;
import java.util.stream.Stream;

public class StreamUtils {

    public static <C> Stream<C> nullToEmpty(C[] array) {
        return array == null ? Stream.empty() : Arrays.stream(array);
    }
}

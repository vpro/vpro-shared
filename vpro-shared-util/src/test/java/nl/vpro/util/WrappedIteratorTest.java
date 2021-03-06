package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class WrappedIteratorTest {

    @Test
    public void test() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", null, "d"));
        WrappedIterator<String, String> wrapped = new WrappedIterator<String, String>(list.iterator()) {


            @Override
            public String next() {
                return "{" + wrapped.next() + "}";
            }
        };
        StringBuilder build = new StringBuilder();
        while(wrapped.hasNext()) {
            String s = wrapped.next();
            build.append(s);
            if ("{b}".equals(s)) {
                wrapped.remove();
            }
        }
        assertEquals("{a}{b}{c}{null}{d}", build.toString());
        assertEquals(Arrays.asList("a", "c", null, "d"), list);
    }
}

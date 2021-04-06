package nl.vpro.test.jqwik;

import net.jqwik.api.Assume;

import org.hamcrest.Matcher;

public class HamcrestSupport {


    public static  <T> void assumeThat(T value, Matcher<T> matcher) {
        Assume.that(matcher.matches(value));
    }

    public static  <T> void assumeNotNull(T data) {
        Assume.that(data != null);
    }
    public static  void assumeTrue(boolean b) {
        Assume.that(b);
    }
}

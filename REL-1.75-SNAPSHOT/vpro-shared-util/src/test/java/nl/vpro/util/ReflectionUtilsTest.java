package nl.vpro.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 */
public class ReflectionUtilsTest {


    public static class AWithOutSetters {
        public String a = "A";
        public Integer b;

        public AWithOutSetters() {
        }

    }

    public static Map<String, String> properties = new HashMap<>();
    static {
        properties.put("a", "B");
        properties.put("b", "3");
    }

    @Test
    public void testOnlyIfNull() {
        AWithOutSetters a = new AWithOutSetters();
        ReflectionUtils.configureIfNull(a, properties);
        assertThat(a.a).isEqualTo("A");
        assertThat(a.b).isEqualTo(3);

    }


    @Test
    public void testOnField() {
        AWithOutSetters a = new AWithOutSetters();
        ReflectionUtils.configured(a, properties);
        assertThat(a.a).isEqualTo("B");
        assertThat(a.b).isEqualTo(3);

    }

}

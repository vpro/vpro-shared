package nl.vpro.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderedPropertiesTest {

    Properties properties = new OrderedProperties();

    {
        properties.put("x", "X");
        properties.put("y", "Y");
        properties.put("z", "Z");
        properties.put("a", "A");
    }

    @Test
    void string() {
        assertThat(properties.toString()).isEqualTo("{x=X, y=Y, z=Z, a=A}");
    }

    @Test
    void keys() {
        assertThat(Collections.list(properties.keys())).containsExactly("x", "y", "z", "a");
    }

    @SuppressWarnings("unchecked")
    @Test
    void propertyNames() {
        assertThat((Collection<Object>) Collections.list(properties.propertyNames())).containsExactly("x", "y", "z", "a");
    }

    @Test
    void values() {
        assertThat(properties.values()).containsExactly("X", "Y", "Z", "A");
    }

    @Test
    void keySet() {
        assertThat(properties.keySet()).containsExactly("x", "y", "z", "a");
    }

    @Test
    void forEach() {
        StringBuilder builder = new StringBuilder();
        properties.forEach((k, v) -> builder.append(k).append("=").append(v).append("\n"));
        assertThat(builder.toString()).isEqualTo("x=X\ny=Y\nz=Z\na=A\n");
    }

    @Test
    void remove() {
        properties.remove("y");
        assertThat(properties.toString()).isEqualTo("{x=X, z=Z, a=A}");
    }

    @Test
    void reput() {
        properties.put("y", "YY");
        assertThat(properties.toString()).isEqualTo("{x=X, z=Z, a=A, y=YY}");
    }

}

package nl.vpro.util;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static nl.vpro.util.Env.PROD;
import static nl.vpro.util.Env.TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.41
 */
public class ReflectionUtilsTest {

    public static class AWithLombok {
        private String a = "A";
        private Integer b;
        @Builder
        public AWithLombok(String a, Integer b) {
            this.a = a;
            this.b = b;
        }

    }

    public static class AWithSetters {
        private String a = "A";
        private Integer b;

        public AWithSetters() {
        }

        public void setA(String a) {
            this.a = a;
        }

        public void setB(Integer b) {
            this.b = b;
        }
    }


    Map<String, String> properties;

    @Before
    public void init() {
        properties = new HashMap<>();
        properties.put("a", "1");
        properties.put("a.test", "2");
        properties.put("a.prod", "3");
        properties.put("b.test", "1");
        properties.put("b.prod", "2");
        properties.put("c", "1");
    }
    @Test
    public void testFilteredANull() {
        assertThat(ReflectionUtils.filtered(null, properties).get("a")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).get("a.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).get("a.prod")).isNull();
    }

    @Test
    public void testFilteredATest() {
        assertThat(ReflectionUtils.filtered(TEST, properties).get("a")).isEqualTo("2");
        assertThat(ReflectionUtils.filtered(TEST, properties).get("a.test")).isNull();
        assertThat(ReflectionUtils.filtered(TEST, properties).get("a.prod")).isNull();
    }

    @Test
    public void testFilteredAProd() {
        assertThat(ReflectionUtils.filtered(PROD, properties).get("a")).isEqualTo("3");
        assertThat(ReflectionUtils.filtered(PROD, properties).get("a.test")).isNull();
        assertThat(ReflectionUtils.filtered(PROD, properties).get("a.prod")).isNull();
    }

    @Test
    public void testFilteredBNull() {
        assertThat(ReflectionUtils.filtered(null, properties).get("b")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).get("b.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).get("b.prod")).isNull();
    }

    @Test
    public void testFilteredBTest() {
        assertThat(ReflectionUtils.filtered(TEST, properties).get("b")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(TEST, properties).get("b.test")).isNull();
        assertThat(ReflectionUtils.filtered(TEST, properties).get("b.prod")).isNull();
    }

    @Test
    public void testFilteredBProd() {
        assertThat(ReflectionUtils.filtered(PROD, properties).get("b")).isEqualTo("2");
        assertThat(ReflectionUtils.filtered(PROD, properties).get("b.test")).isNull();
        assertThat(ReflectionUtils.filtered(PROD, properties).get("b.prod")).isNull();
    }

    @Test
    public void testFilteredCNull() {
        assertThat(ReflectionUtils.filtered(null, properties).get("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).get("c.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).get("c.prod")).isNull();
    }

    @Test
    public void testFilteredCTest() {
        assertThat(ReflectionUtils.filtered(TEST, properties).get("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(TEST, properties).get("c.test")).isNull();
        assertThat(ReflectionUtils.filtered(TEST, properties).get("c.prod")).isNull();
    }

    @Test
    public void testFilteredCProd() {
        assertThat(ReflectionUtils.filtered(PROD, properties).get("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(PROD, properties).get("c.test")).isNull();
        assertThat(ReflectionUtils.filtered(PROD, properties).get("c.prod")).isNull();
    }

    @Test
    public void testConfigured() {
        AWithSetters a = new AWithSetters();
        ReflectionUtils.configured(Env.PROD, a, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithFile() {
        AWithSetters a = new AWithSetters();
        ReflectionUtils.configured(Env.PROD, a, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithLombok() {
        AWithLombok a = ReflectionUtils.configured(Env.PROD, AWithLombok.class, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }


    @Test
    public void testConfiguredWithLombokAndFile() {
        AWithLombok a = ReflectionUtils.configured(Env.PROD, AWithLombok.class, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithConstructor() {
        AWithSetters a = ReflectionUtils.configured(Env.PROD, AWithSetters.class, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithConstructorAndDefaultEnv() {
        AWithSetters a = ReflectionUtils.configured(AWithSetters.class, properties);
        assertThat(a.a).isEqualTo("2");
        assertThat(a.b).isEqualTo(1);
    }


    @Test
    public void testConfiguredWithLomokAndFileAndDefaultEnv() {
        AWithLombok a = ReflectionUtils.configured(AWithLombok.class, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }



}

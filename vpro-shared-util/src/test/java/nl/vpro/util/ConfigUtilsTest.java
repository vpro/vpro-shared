package nl.vpro.util;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static nl.vpro.util.Env.PROD;
import static nl.vpro.util.Env.TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
public class ConfigUtilsTest {


    public enum E {
        e,
        f;
    }

    public static class AWithLombok {
        String a = "A";
        Integer b;
        List<E> enums;

        @Builder
        public AWithLombok(String a, Integer b, List<E> enums) {
            this.a = a;
            this.b = b;
            this.enums = enums;
        }

    }

    public static class AWithSetters {
        String a = "A";
        Integer b;

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
        properties.put("enums", "e,f");
    }

    @Test
    public void testFilteredANull() {
        assertThat(ConfigUtils.filtered(null, properties).get("a")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(null, properties).get("a.test")).isNull();
        assertThat(ConfigUtils.filtered(null, properties).get("a.prod")).isNull();

    }

    @Test
    public void testFilteredATest() {
        assertThat(ConfigUtils.filtered(TEST, properties).get("a")).isEqualTo("2");
        assertThat(ConfigUtils.filtered(TEST, properties).get("a.test")).isNull();
        assertThat(ConfigUtils.filtered(TEST, properties).get("a.prod")).isNull();
    }

    @Test
    public void testFilteredAProd() {
        assertThat(ConfigUtils.filtered(PROD, properties).get("a")).isEqualTo("3");
        assertThat(ConfigUtils.filtered(PROD, properties).get("a.test")).isNull();
        assertThat(ConfigUtils.filtered(PROD, properties).get("a.prod")).isNull();
    }

    @Test
    public void testFilteredBNull() {
        assertThat(ConfigUtils.filtered(null, properties).get("b")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(null, properties).get("b.test")).isNull();
        assertThat(ConfigUtils.filtered(null, properties).get("b.prod")).isNull();
    }

    @Test
    public void testFilteredBTest() {
        assertThat(ConfigUtils.filtered(TEST, properties).get("b")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(TEST, properties).get("b.test")).isNull();
        assertThat(ConfigUtils.filtered(TEST, properties).get("b.prod")).isNull();
    }

    @Test
    public void testFilteredBProd() {
        assertThat(ConfigUtils.filtered(PROD, properties).get("b")).isEqualTo("2");
        assertThat(ConfigUtils.filtered(PROD, properties).get("b.test")).isNull();
        assertThat(ConfigUtils.filtered(PROD, properties).get("b.prod")).isNull();
    }

    @Test
    public void testFilteredCNull() {
        assertThat(ConfigUtils.filtered(null, properties).get("c")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(null, properties).get("c.test")).isNull();
        assertThat(ConfigUtils.filtered(null, properties).get("c.prod")).isNull();
    }

    @Test
    public void testFilteredCTest() {
        assertThat(ConfigUtils.filtered(TEST, properties).get("c")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(TEST, properties).get("c.test")).isNull();
        assertThat(ConfigUtils.filtered(TEST, properties).get("c.prod")).isNull();
    }

    @Test
    public void testFilteredCProd() {
        assertThat(ConfigUtils.filtered(PROD, properties).get("c")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(PROD, properties).get("c.test")).isNull();
        assertThat(ConfigUtils.filtered(PROD, properties).get("c.prod")).isNull();
    }

    @Test
    public void testConfigured() {
        AWithSetters a = new AWithSetters();
        ConfigUtils.configured(Env.PROD, a, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithFile() {
        AWithSetters a = new AWithSetters();
        ConfigUtils.configured(Env.PROD, a, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithLombok() {
        AWithLombok a = ConfigUtils.configured(Env.PROD, AWithLombok.class, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
        assertThat(a.enums).containsExactly(E.e, E.f);
    }


    @Test
    public void testConfiguredWithLombokAndFile() {
        AWithLombok a = ConfigUtils.configured(Env.PROD, AWithLombok.class, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithConstructor() {
        AWithSetters a = ConfigUtils.configured(Env.PROD, AWithSetters.class, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    public void testConfiguredWithConstructorAndDefaultEnv() {
        AWithSetters a = ConfigUtils.configured(AWithSetters.class, properties);
        assertThat(a.a).isEqualTo("2");
        assertThat(a.b).isEqualTo(1);
    }


    @Test
    public void testConfiguredWithLomokAndFileAndDefaultEnv() {
        AWithLombok a = ConfigUtils.configured(AWithLombok.class, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }



}

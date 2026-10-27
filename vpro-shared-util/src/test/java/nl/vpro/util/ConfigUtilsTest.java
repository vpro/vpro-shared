package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static nl.vpro.util.Env.PROD;
import static nl.vpro.util.Env.TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
class ConfigUtilsTest {


    public enum E {
        e,
        f
    }

    public static class AWithLombok {
        String a = "A";
        Integer b;
        List<E> enums;

        @lombok.Builder
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

    public static class AWithOutSetters {
        public String a = "A";
        public Integer b;

        public AWithOutSetters() {
        }

    }


    Map<String, String> properties;

    Map<String, String> propertiesWithPrefix;


    @BeforeEach
    public void init() {
        properties = new HashMap<>();
        properties.put("a", "1");
        properties.put("a.test", "2");
        properties.put("a.prod", "3");

        properties.put("b.test", "1");
        properties.put("b.prod", "2");

        properties.put("c", "1");
        properties.put("enums", "e,f");

        propertiesWithPrefix = new HashMap<>();
        propertiesWithPrefix.put("abc.a", "1");
        propertiesWithPrefix.put("abc.a.test", "2");
        propertiesWithPrefix.put("abc.a.prod", "3");

        propertiesWithPrefix.put("abc.b.test", "1");
        propertiesWithPrefix.put("abc.b.prod", "2");
        propertiesWithPrefix.put("abc.c", "1");
        propertiesWithPrefix.put("xyz.enums", "e,f");
    }

    @Test
    void filteredANull() {
        assertThat(ConfigUtils.filtered(null, properties).get("a")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(null, properties).get("a.test")).isNull();
        assertThat(ConfigUtils.filtered(null, properties).get("a.prod")).isNull();

    }

    @Test
    void filteredATest() {
        assertThat(ConfigUtils.filtered(TEST, properties).get("a")).isEqualTo("2");
        assertThat(ConfigUtils.filtered(TEST, properties).get("a.test")).isNull();
        assertThat(ConfigUtils.filtered(TEST, properties).get("a.prod")).isNull();
    }

    @Test
    void filteredAProd() {
        assertThat(ConfigUtils.filtered(PROD, properties).get("a")).isEqualTo("3");
        assertThat(ConfigUtils.filtered(PROD, properties).get("a.test")).isNull();
        assertThat(ConfigUtils.filtered(PROD, properties).get("a.prod")).isNull();
    }

    @Test
    void filteredBNull() {
        assertThat(ConfigUtils.filtered(null, properties).get("b")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(null, properties).get("b.test")).isNull();
        assertThat(ConfigUtils.filtered(null, properties).get("b.prod")).isNull();
    }

    @Test
    void filteredBTest() {
        assertThat(ConfigUtils.filtered(TEST, properties).get("b")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(TEST, properties).get("b.test")).isNull();
        assertThat(ConfigUtils.filtered(TEST, properties).get("b.prod")).isNull();
    }

    @Test
    void filteredBProd() {
        assertThat(ConfigUtils.filtered(PROD, properties).get("b")).isEqualTo("2");
        assertThat(ConfigUtils.filtered(PROD, properties).get("b.test")).isNull();
        assertThat(ConfigUtils.filtered(PROD, properties).get("b.prod")).isNull();
    }

    @Test
    void filteredCNull() {
        assertThat(ConfigUtils.filtered(null, properties).get("c")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(null, properties).get("c.test")).isNull();
        assertThat(ConfigUtils.filtered(null, properties).get("c.prod")).isNull();
    }

    @Test
    void filteredCTest() {
        assertThat(ConfigUtils.filtered(TEST, properties).get("c")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(TEST, properties).get("c.test")).isNull();
        assertThat(ConfigUtils.filtered(TEST, properties).get("c.prod")).isNull();
    }

    @Test
    void filteredCProd() {
        assertThat(ConfigUtils.filtered(PROD, properties).get("c")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(PROD, properties).get("c.test")).isNull();
        assertThat(ConfigUtils.filtered(PROD, properties).get("c.prod")).isNull();
    }


    @Test
    void filteredWitPrefixA() {
        assertThat(ConfigUtils.filtered(PROD, "abc", propertiesWithPrefix).get("a")).isEqualTo("3");
    }

    @Test
    void filteredWitPrefixB() {
        assertThat(ConfigUtils.filtered(PROD, "abc", propertiesWithPrefix).get("b")).isEqualTo("2");
    }

    @Test
    void filteredWitPrefixC() {
        assertThat(ConfigUtils.filtered(PROD, "abc", propertiesWithPrefix).get("c")).isEqualTo("1");
        assertThat(ConfigUtils.filtered(PROD, "abc", propertiesWithPrefix).get("c.test")).isNull();
        assertThat(ConfigUtils.filtered(PROD, "abc", propertiesWithPrefix).get("c.prod")).isNull();
    }

    @Test
    void configured() {
        AWithSetters a = new AWithSetters();
        ConfigUtils.configured(Env.PROD, a, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }


    @Test
    void configuredWithoutSetter() {
        AWithOutSetters a = new AWithOutSetters();
        ConfigUtils.configured(Env.PROD, a, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }


    @Test
    void configuredWithFile() {
        AWithSetters a = new AWithSetters();
        ConfigUtils.configured(Env.PROD, a, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    void configuredWithLombok() {
        AWithLombok a = ConfigUtils.configured(Env.PROD, AWithLombok.class, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
        assertThat(a.enums).containsExactly(E.e, E.f);
    }


    @Test
    void configuredWithLombokAndFile() {
        AWithLombok a = ConfigUtils.configured(Env.PROD, AWithLombok.class, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    void configuredWithConstructor() {
        AWithSetters a = ConfigUtils.configured(Env.PROD, AWithSetters.class, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

    @Test
    void configuredWithConstructorAndDefaultEnv() {
        AWithSetters a = ConfigUtils.configured(AWithSetters.class, properties);
        assertThat(a.a).isEqualTo("2");
        assertThat(a.b).isEqualTo(1);
    }


    @Test
    void configuredWithLomokAndFileAndDefaultEnv() {
        AWithLombok a = ConfigUtils.configured(AWithLombok.class, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }



}

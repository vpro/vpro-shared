package nl.vpro.util;

import lombok.Builder;

import java.util.Properties;

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


    Properties properties;

    @Before
    public void init() {
        properties = new Properties();
        properties.put("a", "1");
        properties.put("a.test", "2");
        properties.put("a.prod", "3");
        properties.put("b.test", "1");
        properties.put("b.prod", "2");
        properties.put("c", "1");
    }
    @Test
    public void testFilteredANull() {
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("a")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("a.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("a.prod")).isNull();
    }

    @Test
    public void testFilteredATest() {
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("a")).isEqualTo("2");
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("a.test")).isNull();
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("a.prod")).isNull();
    }

    @Test
    public void testFilteredAProd() {
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("a")).isEqualTo("3");
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("a.test")).isNull();
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("a.prod")).isNull();
    }

    @Test
    public void testFilteredBNull() {
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("b")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("b.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("b.prod")).isNull();
    }

    @Test
    public void testFilteredBTest() {
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("b")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("b.test")).isNull();
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("b.prod")).isNull();
    }

    @Test
    public void testFilteredBProd() {
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("b")).isEqualTo("2");
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("b.test")).isNull();
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("b.prod")).isNull();
    }

    @Test
    public void testFilteredCNull() {
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("c.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("c.prod")).isNull();
    }

    @Test
    public void testFilteredCTest() {
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("c.test")).isNull();
        assertThat(ReflectionUtils.filtered(TEST, properties).getProperty("c.prod")).isNull();
    }

    @Test
    public void testFilteredCProd() {
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("c.test")).isNull();
        assertThat(ReflectionUtils.filtered(PROD, properties).getProperty("c.prod")).isNull();


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
        AWithLombok a = ReflectionUtils.lombok(Env.PROD, AWithLombok.class, properties);
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }


    @Test
    public void testConfiguredWithLombokAndFile() {
        AWithLombok a = ReflectionUtils.lombok(Env.PROD, AWithLombok.class, "classpath:/reflectionutilstest.properties");
        assertThat(a.a).isEqualTo("3");
        assertThat(a.b).isEqualTo(2);
    }

}

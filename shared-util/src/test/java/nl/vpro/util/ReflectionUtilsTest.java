package nl.vpro.util;

import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.41
 */
public class ReflectionUtilsTest {

    @Test
    public void testFiltered() {
        Properties properties = new Properties();
        properties.put("a", "1");
        properties.put("a.test", "2");
        properties.put("a.prod", "3");
        properties.put("b.test", "1");
        properties.put("b.prod", "2");
        properties.put("c", "1");

        assertThat(ReflectionUtils.filtered(null, properties).getProperty("a")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("a.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("a.prod")).isNull();
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("a")).isEqualTo("2");
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("a.test")).isNull();
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("a.prod")).isNull();
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("a")).isEqualTo("3");
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("a.test")).isNull();
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("a.prod")).isNull();

        assertThat(ReflectionUtils.filtered(null, properties).getProperty("b")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("b.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("b.prod")).isNull();
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("b")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("b.test")).isNull();
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("b.prod")).isNull();
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("b")).isEqualTo("2");
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("b.test")).isNull();
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("b.prod")).isNull();

        assertThat(ReflectionUtils.filtered(null, properties).getProperty("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("c.test")).isNull();
        assertThat(ReflectionUtils.filtered(null, properties).getProperty("c.prod")).isNull();
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("c.test")).isNull();
        assertThat(ReflectionUtils.filtered("test", properties).getProperty("c.prod")).isNull();
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("c")).isEqualTo("1");
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("c.test")).isNull();
        assertThat(ReflectionUtils.filtered("prod", properties).getProperty("c.prod")).isNull();


    }


}

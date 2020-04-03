package nl.vpro.spring.converters;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringToIntegerListConverterTest {

    @Test
    public void testConvert() {
        assertThat(StringToIntegerListConverter.INSTANCE.convert("1,2")).isEqualTo(Arrays.asList(1, 2));
        assertThat(StringToIntegerListConverter.INSTANCE.convert("1-2")).isEqualTo(Arrays.asList(1, 2));
        assertThat(StringToIntegerListConverter.INSTANCE.convert("1, 7, 23-25")).isEqualTo(Arrays.asList(1, 7, 23, 24, 25));
        assertThat(StringToIntegerListConverter.INSTANCE.convert("1, 7, 23-25, -8")).isEqualTo(Arrays.asList(1, 7, 23, 24, 25, -8));
        assertThat(StringToIntegerListConverter.INSTANCE.convert("-1,1")).isEqualTo(Arrays.asList(-1, 1));
    }
    @Test
    public void testConvertNegativeFirst() {
        assertThat(StringToIntegerListConverter.INSTANCE.convert("-1-1")).isEqualTo(Arrays.asList(-1, 0, 1));
    }

    @Test
    public void testConvertWithDots() {
        assertThat(StringToIntegerListConverter.INSTANCE.convert("-1..1")).isEqualTo(Arrays.asList(-1, 0, 1));
    }
      @Test
    public void testConvertWithDotsAndAll() {
        assertThat(StringToIntegerListConverter.INSTANCE.convert("-20--18,-8..-6,-1..1,3,5, 8-10"))
            .isEqualTo(Arrays.asList(-20, -19, -18, -8, -7, -6, -1, 0, 1, 3, 5, 8, 9, 10));
    }
    @Test
    public void testConvertNegativeBoth() {
        assertThat(StringToIntegerListConverter.INSTANCE.convert("-2--1")).isEqualTo(Arrays.asList(-2, -1));
    }
}

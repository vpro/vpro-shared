package nl.vpro.spring.converters;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;

import static nl.vpro.spring.converters.StringToIntegerListConverter.INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

public class StringToIntegerListConverterTest {

    @Test
    public void testConvert() {
        assertThat(INSTANCE.convert("1,2")).isEqualTo(Arrays.asList(1, 2));
        assertThat(INSTANCE.convert("1-2")).isEqualTo(Arrays.asList(1, 2));
        assertThat(INSTANCE.convert("1, 7, 23-25")).isEqualTo(Arrays.asList(1, 7, 23, 24, 25));
        assertThat(INSTANCE.convert("1, 7, 23-25, -8")).isEqualTo(Arrays.asList(1, 7, 23, 24, 25, -8));
        assertThat(INSTANCE.convert("-1,1")).isEqualTo(Arrays.asList(-1, 1));
    }
    @Test
    public void testConvertNegativeFirst() {
        assertThat(INSTANCE.convert("-1-1")).isEqualTo(Arrays.asList(-1, 0, 1));
    }

    @Test
    public void testConvertWithDots() {
        assertThat(INSTANCE.convert("-1..1")).isEqualTo(Arrays.asList(-1, 0, 1));
    }
      @Test
    public void testConvertWithDotsAndAll() {
        assertThat(INSTANCE.convert("-20--18,-8..-6,-1..1,3,5, 8-10"))
            .isEqualTo(Arrays.asList(-20, -19, -18, -8, -7, -6, -1, 0, 1, 3, 5, 8, 9, 10));
    }
    @Test
    public void testConvertNegativeBoth() {
        assertThat(INSTANCE.convert("-2--1")).isEqualTo(Arrays.asList(-2, -1));
    }

    static class TestClass {
        public String source;

        public Integer sourceNotMatches;


        public List<Integer> target;

        public List<String> targetNotMatches;

        public Map<String, Integer> targetNotMatches2;


    }
    @Test
    public void matches() throws NoSuchFieldException {
        TypeDescriptor source = new TypeDescriptor(TestClass.class.getField("source"));
        TypeDescriptor sourceNotMatches = new TypeDescriptor(TestClass.class.getField("sourceNotMatches"));

        TypeDescriptor target = new TypeDescriptor(TestClass.class.getField("target"));
        TypeDescriptor targetNotMatches = new TypeDescriptor(TestClass.class.getField("targetNotMatches"));
        TypeDescriptor targetNotMatches2 = new TypeDescriptor(TestClass.class.getField("targetNotMatches2"));

        assertThat(INSTANCE.matches(source, target)).isTrue();

        assertThat(INSTANCE.matches(source, targetNotMatches)).isFalse();
        assertThat(INSTANCE.matches(source, targetNotMatches2)).isFalse();


        assertThat(INSTANCE.matches(sourceNotMatches, target)).isFalse();
    }
}

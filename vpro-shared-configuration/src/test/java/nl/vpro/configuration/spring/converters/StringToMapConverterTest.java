package nl.vpro.configuration.spring.converters;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringToMapConverterTest {

    StringToMapConverter converter = new StringToMapConverter();

    @Test
    public void testEmpty() {
        assertThat(converter.convert("")).isEmpty();
        assertThat(converter.convert(null)).isEmpty();
        assertThat(converter.convert(" \n ")).isEmpty();
    }


    @Test
    public void testSimple() {
        assertThat(converter.convert("a=b,c=d")).isEqualTo(Map.of("a", "b", "c", "d"));
    }

}

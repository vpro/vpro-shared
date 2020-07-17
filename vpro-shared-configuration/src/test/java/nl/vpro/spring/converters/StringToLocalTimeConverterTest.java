package nl.vpro.spring.converters;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.13
 */
class StringToLocalTimeConverterTest {

    @Test
    public void test() {
        assertThat(new StringToLocalTimeConverter().convert("8:00")).isEqualTo(LocalTime.of(8, 0));
        assertThat(new StringToLocalTimeConverter().convert("08:00")).isEqualTo(LocalTime.of(8, 0));

    }

}

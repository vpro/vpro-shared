package nl.vpro.spring.converters;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import nl.vpro.configuration.spring.converters.StringToLocalTimeConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.13
 */
class StringToLocalTimeConverterTest {
    StringToLocalTimeConverter instance = new StringToLocalTimeConverter();

    @Test
    public void test() {

        assertThat(instance.convert("8:00")).isEqualTo(LocalTime.of(8, 0));
        assertThat(instance.convert("08:00")).isEqualTo(LocalTime.of(8, 0));

        assertThat(instance.convert("08:00").getHour()).isEqualTo(8);
        assertThat(instance.convert("8:00").getHour()).isEqualTo(8);
        assertThat(instance.convert("8:01").getMinute()).isEqualTo(1);

        assertThatThrownBy(() -> instance.convert("8:1")).isInstanceOf(DateTimeParseException.class);
        assertThatThrownBy(() -> instance.convert(("8:01:31"))).isInstanceOf(DateTimeParseException.class);

    }

}

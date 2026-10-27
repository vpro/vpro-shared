package nl.vpro.xml.bind;

import java.time.LocalDate;
import java.time.Year;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.30
 */
class LocalDateXmlAdapterTest {

    private final LocalDateXmlAdapter instance = new LocalDateXmlAdapter();

    @Test
    void unmarshalLocalDate() {
        assertThat(instance.unmarshal("2015-12-09")).isEqualTo(LocalDate.of(2015, 12, 9));
        assertThat(instance.unmarshal("2015")).isEqualTo(Year.of(2015));

    }

    @Test
    void marshal() {

    }
}

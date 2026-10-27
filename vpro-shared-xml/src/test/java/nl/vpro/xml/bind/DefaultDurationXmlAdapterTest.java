package nl.vpro.xml.bind;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
class DefaultDurationXmlAdapterTest {

    DefaultDurationXmlAdapter adapter = new DefaultDurationXmlAdapter();

    @Test
    void adapt() {
        assertThat(adapter.marshal(Duration.ofHours(3))).isEqualTo("PT3H");
    }


    @Test
    void adaptLong() {
        assertThat(adapter.marshal(Duration.ofDays(30).plus(Duration.ofHours(4))).toString()).isEqualTo("PT724H");
    }
}

package nl.vpro.xml.bind;

import java.time.Duration;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class DefaultDurationXmlAdapterTest {

    DefaultDurationXmlAdapter adapter = new DefaultDurationXmlAdapter();

    @Test
    public void adapt() {
        assertThat(adapter.marshal(Duration.ofHours(3))).isEqualTo("PT3H");
    }


    @Test
    public void adaptLong() {
        assertThat(adapter.marshal(Duration.ofDays(30).plus(Duration.ofHours(4))).toString()).isEqualTo("PT724H");
    }
}

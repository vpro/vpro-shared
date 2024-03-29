package nl.vpro.xml.bind;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class DurationXmlAdapterTest {


    DurationXmlAdapter adapter = new DurationXmlAdapter();

    @Test
    public void adapt() {
        assertThat(adapter.marshal(Duration.ofHours(3)).toString()).isEqualTo("P0DT3H0M0.000S");
    }


    @Test
    public void adaptLong() {
        assertThat(adapter.marshal(Duration.ofDays(30).plus(Duration.ofHours(4))).toString()).isEqualTo("P0Y0M30DT4H0M0.000S");
    }

    @Test
    public void veryLong() {
        javax.xml.datatype.Duration xmlDuration = DurationXmlAdapter.DATATYPE_FACTORY.newDuration(Duration.ofDays(800).toMillis());
        Duration d = adapter.unmarshal(xmlDuration);
        assertThat(d).isEqualTo(Duration.ofDays(800));
    }

}

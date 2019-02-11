package nl.vpro.xml.bind;

import java.time.LocalDate;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.5
 */
public class ZonedLocalDateXmlAdapterTest {


    ZonedLocalDateXmlAdapter instance = new ZonedLocalDateXmlAdapter();
    @Test
    public void unmarshal() {

        assertThat(instance.unmarshal("2019-02-11+01:00")).isEqualTo(LocalDate.of(2019, 2, 11));
    }

    @Test
    public void marshal() throws Exception {
        assertThat(instance.marshal(LocalDate.of(2019, 2, 11))).isEqualTo("2019-02-11+01:00");
    }
}

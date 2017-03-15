package nl.vpro.xml.bind;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 */
@RunWith(Parameterized.class)
public class InstantXmlAdapterTest {

    private Instant in;
    private String xml;
    private String xmlomitisfalse;
    private boolean marshal;

    InstantXmlAdapter instance = new InstantXmlAdapter();

    static Object[][] CASES = {
        // Instant                   XML-value
        {"2016-01-20T10:48:09.954Z", "2016-01-20T11:48:09.954+01:00", null, true},
        {LocalDateTime.of(2016, 1, 20, 11, 48, 9, 500000).atZone(ZoneId.of("Europe/Amsterdam")).toInstant(), "2016-01-20T11:48:09.001+01:00", null, true},
        {"2016-01-20T10:48:00.000Z", "2016-01-20T11:48:00+01:00", "2016-01-20T11:48:00.000+01:00", true},
        {"2016-01-20T10:59:00.733Z", "1453287540733", null, false},
        {"2016-01-19T23:00:00Z", "2016-01-20", null, false},
        {"2016-01-19T23:00:00Z", "2016-01-20T00:00:00", null, false}
    };


    public InstantXmlAdapterTest(Object in,
                                 String xml,
                                 String xml2,
                                 boolean marshal) {
        this.in = in instanceof Instant ? (Instant) in : Instant.parse(String.valueOf(in));
        this.xml = xml;
        if (xml2 == null) {
            xmlomitisfalse = xml;
        } else {
            xmlomitisfalse = xml2;
        }
        this.marshal = marshal;
    }

    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(CASES);
    }


    @Test
    public void testMarshal() {
        Assume.assumeTrue(marshal);
        InstantXmlAdapter.OMIT_MILLIS_IF_ZERO.remove();
        assertThat(instance.marshal(in)).isEqualTo(xml);
        InstantXmlAdapter.OMIT_MILLIS_IF_ZERO.set(false);
        assertThat(instance.marshal(in)).isEqualTo(xmlomitisfalse);

    }
    @Test
    public void testUnmarshal() {
        assertThat(instance.unmarshal(xml)).isEqualTo(in.plusNanos(500000).truncatedTo(ChronoUnit.MILLIS));
        assertThat(instance.unmarshal(xmlomitisfalse)).isEqualTo(in.plusNanos(500000).truncatedTo(ChronoUnit.MILLIS));
    }

}

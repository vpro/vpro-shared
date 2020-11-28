package nl.vpro.xml.bind;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Michiel Meeuwissen
 * @since 0.32
 */
public class InstantXmlAdapterTest {


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


    public static Stream<Object[]> parameters() {
        return Arrays.stream(CASES).map(
            (c) -> {
                Object in = c[0];
                String xml = (String) c[1];
                String xml2 = (String) c[2];
                boolean marshal = (boolean) c[3];
                return new Object[] {
                    in instanceof Instant ? (Instant) in : Instant.parse(String.valueOf(in)),
                    xml,
                    xml2 == null ? xml : xml2,
                    marshal
                };
            }
        );
    }


    @ParameterizedTest
    @MethodSource("parameters")
    public void testMarshal(
        Instant in,
        String xml,
        String xmlomitisfalse,
        boolean marshal
    ) {
        assumeTrue(marshal);
        InstantXmlAdapter.OMIT_MILLIS_IF_ZERO.remove();
        assertThat(instance.marshal(in)).isEqualTo(xml);
        InstantXmlAdapter.OMIT_MILLIS_IF_ZERO.set(false);
        assertThat(instance.marshal(in)).isEqualTo(xmlomitisfalse);

    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testUnmarshal(
        Instant in,
        String xml,
        String xmlomitisfalse,
        boolean marshal
    ) {

        assertThat(instance.unmarshal(xml)).isEqualTo(in.plusNanos(500000).truncatedTo(ChronoUnit.MILLIS));
        assertThat(instance.unmarshal(xmlomitisfalse)).isEqualTo(in.plusNanos(500000).truncatedTo(ChronoUnit.MILLIS));
    }

}

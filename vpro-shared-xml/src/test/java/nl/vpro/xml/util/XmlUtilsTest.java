package nl.vpro.xml.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.71
 */
@Slf4j
public class XmlUtilsTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    }


    @Test
    public void toInstant() throws Exception {
        Instant instance = XmlUtils.toInstant(ZoneId.of("Europe/London"), XmlUtils.FACTORY.newXMLGregorianCalendar("2017-06-26T14:23:00+02:00"));
        assertThat(instance).isEqualTo(LocalDateTime.of(2017, 6, 26, 14, 23).atZone(ZoneId.of("Europe/Amsterdam")).toInstant());
        log.info("{}", instance);
    }

    @Test
    public void toInstantImplicitZone() throws Exception {
        Instant instance = XmlUtils.toInstant(ZoneId.of("Europe/Amsterdam"), XmlUtils.FACTORY.newXMLGregorianCalendar("2017-06-26T14:23:00"));
        assertThat(instance).isEqualTo(LocalDateTime.of(2017, 6, 26, 14, 23).atZone(ZoneId.of("Europe/Amsterdam")).toInstant());
        log.info("{}", instance);
    }

    @Test
    public void toXml() throws Exception {

        assertThat(XmlUtils.toXml(ZoneId.of("Europe/Amsterdam"), LocalDateTime.of(2017, 6, 26, 14, 23).atZone(ZoneId.of("Europe/Amsterdam")).toInstant()).toXMLFormat())
            .isEqualTo("2017-06-26T14:23:00.000+02:00");
    }

}

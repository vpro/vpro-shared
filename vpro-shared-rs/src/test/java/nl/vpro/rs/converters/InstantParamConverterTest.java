package nl.vpro.rs.converters;

import java.time.*;
import java.time.format.DateTimeParseException;

import org.junit.Test;

import nl.vpro.util.TimeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class InstantParamConverterTest {

    InstantParamConverter instance = new InstantParamConverter();
    @Test
    public void fromString1() {

        assertThat(instance.fromString("100000")).isEqualTo(Instant.ofEpochMilli(100000));
    }

    @Test
    public void fromString2() {
        assertThat(instance.fromString("2016-06-07T16:17Z")).isEqualTo(ZonedDateTime.of(LocalDate.of(2016, 6, 7), LocalTime.of(16, 17), ZoneId.of("Z")).toInstant());
    }

    @Test
    public void fromString3() {
        assertThat(instance.fromString("2016-07-21T17:10:05+02:00")).isEqualTo(OffsetDateTime.of(LocalDate.of(2016, 7, 21), LocalTime.of(17, 10, 5), ZoneOffset.ofHours(2)).toInstant());
    }

    @Test
    public void fromString4() {
        assertThat(instance.fromString("2016-07-21T17:13:16+02:00[Europe/Amsterdam]")).isEqualTo(ZonedDateTime.of(LocalDate.of(2016, 7, 21), LocalTime.of(17, 13, 16), ZoneId.of("Europe/Amsterdam")).toInstant());
    }


    @Test
    public void fromString4plus() {
        assertThat(instance.fromString("2016-07-21T17:13:16 02:00[Europe/Amsterdam]")).isEqualTo(ZonedDateTime.of(LocalDate.of(2016, 7, 21), LocalTime.of(17, 13, 16), ZoneId.of("Europe/Amsterdam")).toInstant());
    }


    @Test
    public void fromString5() {
        assertThat(instance.fromString("2000-01-01")).isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), TimeUtils.ZONE_ID).toInstant());
    }

    @Test
    public void fromString6() {
        assertThat(instance.fromString("2000-01-01T18:20")).isEqualTo(LocalDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(18, 20)).atZone(TimeUtils.ZONE_ID).toInstant());
    }

    @Test(expected= DateTimeParseException.class)
    public void fromError() {
        instance.fromString("20aaa00-01-01");


    }

}

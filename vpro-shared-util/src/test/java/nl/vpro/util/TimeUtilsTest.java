package nl.vpro.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.45
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
public class TimeUtilsTest {


    @Test
    public void parseZoned() throws Exception {

        assertThat(TimeUtils.parseZoned("2000-01-01").get()).isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), TimeUtils.ZONE_ID));

    }

    @Test
    public void parseZonedYear() throws Exception {
        assertThat(TimeUtils.parseZoned("2000").get()).isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), TimeUtils.ZONE_ID));

    }

    @Test
    public void parseMillis() throws Exception {
        assertThat(TimeUtils.parseZoned("1474643244279").get()).isEqualTo(ZonedDateTime.of(LocalDate.of(2016, 9, 23), LocalTime.of(17, 7, 24, 279000000), TimeUtils.ZONE_ID));

    }

    @Test
    public void parse() throws Exception {

        assertThat(TimeUtils.parse("2000-07-11T14:00:33.556+02:00").get())
            .isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 7, 11), LocalTime.of(14, 0, 33, 556000000), TimeUtils.ZONE_ID).toInstant());

    }

    @Test
    public void parseDuration() throws Exception {
        assertThat(TimeUtils.parseDuration("PT5M").get()).isEqualTo(Duration.ofMinutes(5));
        assertThat(TimeUtils.parseDuration("T5M").get()).isEqualTo(Duration.ofMinutes(5));
        assertThat(TimeUtils.parseDuration("5M").get()).isEqualTo(Duration.ofMinutes(5));
        assertThat(TimeUtils.parseDuration("6s").get()).isEqualTo(Duration.ofSeconds(6));
        assertThat(TimeUtils.parseDuration("7S").get()).isEqualTo(Duration.ofSeconds(7));
        assertThat(TimeUtils.parseDuration("5000").get()).isEqualTo(Duration.ofMillis(5000));
        assertThat(TimeUtils.parseDuration("PT300s").get()).isEqualTo(Duration.ofSeconds(300));

        assertThat(TimeUtils.parseDuration("PT-300s").get()).isEqualTo(Duration.ofSeconds(-300));
        assertThat(TimeUtils.parseDuration("-300s").get()).isEqualTo(Duration.ofSeconds(-300));
        assertThat(TimeUtils.parseDuration("-2M").get()).isEqualTo(Duration.ofSeconds(-120));


        assertThat(TimeUtils.parseDuration("P10D").get()).isEqualTo(Duration.ofHours(240));


        assertThat(TimeUtils.parseDuration("").orElse(null)).isNull();


    }

    @Test
    public void testLarger() {
        assertThat(TimeUtils.isLarger(TimeUtils.parseDuration("PT6M").get(), TimeUtils.parseDuration("PT5M").get())).isTrue();
    }


}

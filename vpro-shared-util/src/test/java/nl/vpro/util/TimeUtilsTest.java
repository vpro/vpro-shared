package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 0.45
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent"})
@Slf4j
public class TimeUtilsTest {


    @Test
    public void parseZoned() {

        assertThat(TimeUtils.parseZoned("2000-01-01").get()).isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), TimeUtils.ZONE_ID));

    }

    @Test
    public void parseZonedYear() {
        assertThat(TimeUtils.parseZoned("2000").get()).isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), TimeUtils.ZONE_ID));

    }

    @Test
    public void parseMillis() {
        assertThat(TimeUtils.parseZoned("1474643244279").get()).isEqualTo(ZonedDateTime.of(LocalDate.of(2016, 9, 23), LocalTime.of(17, 7, 24, 279000000), TimeUtils.ZONE_ID));

    }

    @Test
    public void parse() {

        assertThat(TimeUtils.parse("2000-07-11T14:00:33.556+02:00").get())
            .isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 7, 11), LocalTime.of(14, 0, 33, 556000000), TimeUtils.ZONE_ID).toInstant());

    }

    @Test
    public void parse2() {
        LocalDateTime example = LocalDateTime.of(2018, 2, 13, 9, 0);
        log.info("{}", example);
        assertThat(TimeUtils.parse("2018-02-13T09:00").get())
            .isEqualTo(example.atZone(TimeUtils.ZONE_ID).toInstant());

    }

    @Test
    public void parseDuration() {
        assertThat(TimeUtils.parseDuration("PT5M").get()).isEqualTo(Duration.ofMinutes(5));
        assertThat(TimeUtils.parseDuration("T5M").get()).isEqualTo(Duration.ofMinutes(5));
        assertThat(TimeUtils.parseDuration("5M").get()).isEqualTo(Duration.ofMinutes(5));
        assertThat(TimeUtils.parseDuration("6s").get()).isEqualTo(Duration.ofSeconds(6));
        assertThat(TimeUtils.parseDuration("6 s").get()).isEqualTo(Duration.ofSeconds(6));
        assertThat(TimeUtils.parseDuration("7S").get()).isEqualTo(Duration.ofSeconds(7));
        assertThat(TimeUtils.parseDuration("5000").get()).isEqualTo(Duration.ofMillis(5000));
        assertThat(TimeUtils.parseDuration("PT300s").get()).isEqualTo(Duration.ofSeconds(300));

        assertThat(TimeUtils.parseDuration("PT-300s").get()).isEqualTo(Duration.ofSeconds(-300));
        assertThat(TimeUtils.parseDuration("-300s").get()).isEqualTo(Duration.ofSeconds(-300));
        assertThat(TimeUtils.parseDuration("0.1s").get()).isEqualTo(Duration.ofMillis(100));

        assertThat(TimeUtils.parseDuration("-2M").get()).isEqualTo(Duration.ofSeconds(-120));


        assertThat(TimeUtils.parseDuration("P10D").get()).isEqualTo(Duration.ofHours(240));


        assertThat(TimeUtils.parseDuration("").orElse(null)).isNull();

        assertThatThrownBy(() -> TimeUtils.parseDuration("can'tbeparsed"))
            .isExactlyInstanceOf(DateTimeParseException.class)
            .hasMessage("can'tbeparsed:Text cannot be parsed to a Duration")
            .matches((dtm) -> {
                return ((DateTimeParseException) dtm).getParsedString().equals("can'tbeparsed");
            }, "doest match");


    }

    @Test
    public void parseWeek() {
        assertThat(TimeUtils.parseDuration("P4W").get()).isEqualTo(Duration.ofDays(7 * 4));

    }

    @Test
    public void durationToString() {
        assertThat(TimeUtils.toParsableString(Duration.ofSeconds(5))).isEqualTo("5S");
        assertThat(TimeUtils.toParsableString(Duration.ofDays(50))).isEqualTo("1200H");

    }


    @Test
    public void parseLocalDateTime() {
        assertThat(
            TimeUtils.parseLocalDateTime("2019-02-13T09:16").get()).isEqualTo(LocalDateTime.of(2019,2, 13, 9, 16));

         assertThat(
            TimeUtils.parseLocalDateTime("2019-02-13").get()).isEqualTo(LocalDate.of(2019,2, 13).atStartOfDay());

    }

    @Test
    public void testLarger() {
        assertThat(TimeUtils.isLarger(TimeUtils.parseDuration("PT6M").get(), TimeUtils.parseDuration("PT5M").get())).isTrue();
    }

    @Test
    public void roundMillis() {
        Duration duration = Duration.ofSeconds(4).plusNanos(600_000); // 4 seconds 0.6 ms
        assertThat(duration.toMillis()).isEqualTo(4000);

        System.out.println(duration);
        assertThat(TimeUtils.roundToMillis(duration).toString()).isEqualTo("PT4.001S");
        assertThat(TimeUtils.roundToMillis(duration).toMillis()).isEqualTo(4001);

    }


}

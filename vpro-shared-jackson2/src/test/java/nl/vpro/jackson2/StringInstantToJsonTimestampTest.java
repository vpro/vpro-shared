package nl.vpro.jackson2;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class StringInstantToJsonTimestampTest {


    @Test
    public void testOk() {
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("2017-05-24T16:30:00+02:00");
        assertThat(instant).isEqualTo(
            LocalDateTime.of(2017, 5, 24, 16, 30, 0).atZone(ZoneId.of("Europe/Amsterdam")).toInstant());
        log.debug("{}", instant);
    }

    //@Test(expected = IllegalArgumentException.class)
    @Test // natty does succeed in parsing this
    public void testOdd() {
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("0737-05-22T14:35:55+00:19:32");     // Of course, that is a very odd timezone

        assertThat(instant).isEqualTo(
            LocalDateTime.of(737, 5, 26, 14, 16, 55).atZone(ZoneId.of("UTC")).toInstant());
        log.debug("{}", instant);

    }

    @Test
    public void testNotOk() {
        assertThatThrownBy(() -> {
            StringInstantToJsonTimestamp.parseDateTime("dit is echt geen datum");
        }).isInstanceOf(IllegalArgumentException.class);

    }


    @Test
    public void testNattyNow() {
        assertThat(StringInstantToJsonTimestamp.parseDateTime("now")).isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
    }


}

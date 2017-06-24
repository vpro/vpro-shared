package nl.vpro.jackson2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 */
public class StringInstantToJsonTimestampTest {


    @Test
    public void testOk() {
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("2017-05-24T16:30:00+02:00");
    }

    //@Test(expected = IllegalArgumentException.class)
    @Test // natty does succeed in parsing this
    public void testOdd() {
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("0737-05-22T14:35:55+00:19:32");     // Of course, that is a very odd timezone

        assertThat(instant).isEqualTo(LocalDateTime.of(737, 5, 26, 14, 16, 55).atZone(ZoneId.of("UTC")).toInstant());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotOk() {
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("dit is echt geen datum");

    }


}

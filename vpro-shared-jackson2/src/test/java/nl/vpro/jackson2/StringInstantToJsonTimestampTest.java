package nl.vpro.jackson2;

import java.time.Instant;

import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 */
public class StringInstantToJsonTimestampTest {


    @Test
    public void testOk() {
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("2017-05-24T16:30:00+02:00");
    }
    @Test(expected = IllegalArgumentException.class)
    // Of course, that is a very odd timezone
    public void test() {
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("0737-05-22T14:35:55+00:19:32");
    }



}

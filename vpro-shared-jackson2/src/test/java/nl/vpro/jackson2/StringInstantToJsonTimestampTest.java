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
    @Test
    public void test() {
        Instant.parse("0737-05-22T14:35:55+00:19:32");
        Instant instant = StringInstantToJsonTimestamp.parseDateTime("0737-05-22T14:35:55+00:19:32");
    }



}

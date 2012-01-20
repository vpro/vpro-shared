package nl.vpro.domain.shared.bind;

import java.util.Date;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 */
public class DateToDurationTest {


    @Test
    public void testDayTime() throws Exception {
        DateToDuration dateToDuration = new DateToDuration();
        assertEquals("P0DT0H0M0.000S", dateToDuration.marshalDayTime(0l).toString());
        assertEquals("P0DT0H1M40.000S", dateToDuration.marshalDayTime(100000l).toString());
        assertEquals("P30DT0H0M0.000S", dateToDuration.marshalDayTime(30l * 24 * 60 * 60 * 1000).toString());
    }

    @Test
    public void testTime() throws Exception {
        DateToDuration dateToDuration = new DateToDuration();
        assertEquals("P0Y0M0DT0H0M0.000S", dateToDuration.marshal(0l).toString());
        assertEquals("P0Y0M0DT0H1M40.000S", dateToDuration.marshal(100000l).toString());
        assertEquals("P0Y0M30DT0H0M0.000S", dateToDuration.marshal(30l * 24 * 60 * 60 * 1000).toString());
        assertEquals("P0Y2M1DT0H0M0.000S", dateToDuration.marshal(60l * 24 * 60 * 60 * 1000).toString());
    }

    @Test
    public void test() throws Exception {
        DateToDuration dateToDuration = new DateToDuration();
        assertEquals("P0DT0H0M0.000S", dateToDuration.marshal(new Date(0)).toString());
        assertEquals("P0DT0H1M40.000S", dateToDuration.marshal(new Date(100000l)).toString());
        assertEquals("P0Y2M1DT0H0M0.000S", dateToDuration.marshal(new Date(2l * 30 * 24 * 60 * 60 * 1000)).toString());
        assertEquals("P0Y2M1DT0H0M0.000S", dateToDuration.marshal(new Date(60l * 24 * 60 * 60 * 1000)).toString());
    }

}

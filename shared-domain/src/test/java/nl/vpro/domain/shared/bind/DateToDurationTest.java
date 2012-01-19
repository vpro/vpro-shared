package nl.vpro.domain.shared.bind;

import java.util.Date;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 */
public class DateToDurationTest {

    @Test
    public void test() throws Exception {
        DateToDuration dateToDuration = new DateToDuration();
        assertEquals("P0DT0H0M0.000S", dateToDuration.marshal(new Date(0)).toString());
        assertEquals("P0DT0H1M40.000S", dateToDuration.marshal(new Date(100000l)).toString());
        assertEquals("P0Y2M1DT0H0M0.000S", dateToDuration.marshal(new Date(2l * 30 * 24 * 60 * 60 * 1000)).toString());
    }

}

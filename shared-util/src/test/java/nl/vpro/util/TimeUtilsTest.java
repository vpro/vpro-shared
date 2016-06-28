package nl.vpro.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 0.45
 */
public class TimeUtilsTest {
    @Test
    public void parseZoned() throws Exception {

        assertThat(TimeUtils.parseZoned("2000-01-01").get()).isEqualTo(ZonedDateTime.of(LocalDate.of(2000, 1, 1), LocalTime.of(0, 0), TimeUtils.ZONE_ID));

    }


}

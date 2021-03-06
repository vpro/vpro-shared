package nl.vpro.i18n;

import java.time.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.8
 */
public class DutchTest {


    @Test
    public void formatInstantSmartly() {
        ZonedDateTime now = LocalDateTime.of(2018, 11, 16, 10, 0).atZone(Dutch.ZONE_ID);


        assertThat(Dutch.formatSmartly(now, LocalDateTime.of(2018, 11, 16, 13, 0).atZone(Dutch.ZONE_ID))).isEqualTo("13:00");

        assertThat(Dutch.formatSmartly(now, LocalDateTime.of(2018, 12, 16, 13, 0).atZone(Dutch.ZONE_ID))).isEqualTo("16 december 13:00");

        assertThat(Dutch.formatSmartly(now, LocalDateTime.of(2019, 12, 16, 13, 0).atZone(Dutch.ZONE_ID))).isEqualTo("16 december 2019 13:00");

        Instant realNow = Instant.now();
        LocalTime time = LocalTime.now(Dutch.ZONE_ID);
        assertThat(Dutch.formatSmartly(realNow)).isEqualTo(String.format("%02d:%02d", time.getHour(), time.getMinute()));
    }

}

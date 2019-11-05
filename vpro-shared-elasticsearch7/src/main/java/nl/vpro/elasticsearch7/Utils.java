package nl.vpro.elasticsearch7;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class Utils {


    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSSXXX");

    public  static String formatDateTime(Instant instant) {
        return formatter.format(instant.atZone(ZoneId.of("UTC")));
    }
}

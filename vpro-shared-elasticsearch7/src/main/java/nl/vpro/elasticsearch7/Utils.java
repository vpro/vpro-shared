package nl.vpro.elasticsearch7;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
public class Utils {


    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public  static String formatDateTime(Instant instant) {
        return FORMATTER.format(instant.atZone(ZoneId.of("UTC")));
    }
}

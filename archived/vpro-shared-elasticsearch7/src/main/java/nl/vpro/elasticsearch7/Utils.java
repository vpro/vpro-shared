package nl.vpro.elasticsearch7;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.IOUtils;

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

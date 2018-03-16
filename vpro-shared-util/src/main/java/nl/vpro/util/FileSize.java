package nl.vpro.util;

import java.text.DecimalFormat;

/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class FileSize {


    private static final DecimalFormat format = new DecimalFormat("#.##");
    private static final long KiB = 1024;
    private static final long MiB = 1024 * KiB ;
    private static final long GiB = 1024 * MiB;


    public static String format(long length) {
        if (length > GiB) {
            return format.format(length / GiB) + " GiB";
        }
        if (length > MiB) {
            return format.format(length / MiB) + " MiB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KiB";
        }
        return format.format(length) + " B";
    }

}

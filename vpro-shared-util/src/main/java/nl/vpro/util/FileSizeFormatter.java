package nl.vpro.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.DecimalFormat;

/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
@lombok.Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FileSizeFormatter {

    private static final long KiB = 1024;
    private static final long MiB = 1024 * KiB;
    private static final long GiB = 1024 * MiB;

    private static final long K = 1000;
    private static final long M = 1000 * K;
    private static final long G = 1000 * M;

    @lombok.Builder.Default
    private DecimalFormat format = new DecimalFormat("#.##");

    @lombok.Builder.Default
    private boolean mebi = true;


    public String format(Long length) {
        if (length == null) {
            return "? B";
        }
        if (mebi) {
            return formatMebi(length);
        } else {
            return formatSI(length);
        }
    }

    private String formatMebi(long length) {
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


     private String formatSI(long length) {
        if (length > G) {
            return format.format(length / G) + " GB";
        }
        if (length > M) {
            return format.format(length / M) + " MB";
        }
        if (length > K) {
            return format.format(length / K) + " KB";
        }
        return format.format(length) + " B";
    }

}

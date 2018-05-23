package nl.vpro.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
@lombok.Builder(builderClassName = "Builder")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FileSizeFormatter {

    private static final long KiB = 1024;
    private static final long MiB = 1024 * KiB;
    private static final long GiB = 1024 * MiB;

    private static final long K = 1000;
    private static final long M = 1000 * K;
    private static final long G = 1000 * M;

    public static final DecimalFormatSymbols DECIMAL = DecimalFormatSymbols.getInstance(Locale.US);

    @lombok.Builder.Default
    private DecimalFormat format = new DecimalFormat("#");

    @lombok.Builder.Default
    private boolean mebi = true;

    public static FileSizeFormatter DEFAULT = FileSizeFormatter.builder()
        .pattern("#.#")
        .mebi(true)
        .build();


    public static FileSizeFormatter SI = FileSizeFormatter.builder()
        .pattern("#.#")
        .mebi(false)
        .build();

    public String format(Float length) {
        if (length == null) {
            return "? B";
        }
        if (mebi) {
            return formatMebi(length);
        } else {
            return formatSI(length);
        }
    }

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

    public String formatSpeed(Long length, Duration duration) {
        if (length == null) {
            return format(length) + " /s";
        }
        Float perSecond = 1000f * length / duration.toMillis();
        return format(perSecond) + "/s";
    }



    public String formatSpeed(Long length, Instant start) {
        return formatSpeed(length, Duration.between(start, Instant.now()));
    }

    private String formatMebi(float length) {
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


     private String formatSI(float length) {
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


    public static class Builder {
        public Builder pattern(String pattern) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            decimalFormat.setDecimalFormatSymbols(DECIMAL);
            return format(decimalFormat);
        }
    }

}

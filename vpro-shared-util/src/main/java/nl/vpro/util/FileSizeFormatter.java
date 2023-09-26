package nl.vpro.util;

import lombok.Getter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Formatting file sizes it not always very trivial. This can help.
 * <p>
 * E.g.
 * <pre>{@code
 * FileSizeFormatter.DEFAULT.format(12345)
 * }
 * </pre>
 * For customizing a builder is available (Use {@link #builder()})
 * <p>
 * Find more examples in {@code FileSizeFormatterTest}.
 *
 * @author Michiel Meeuwissen
 * @since 1.76
 */
@Getter
public class FileSizeFormatter {

    private static final long KiB = 1024;
    private static final long MiB = 1024 * KiB;
    private static final long GiB = 1024 * MiB;

    private static final long K = 1000;
    private static final long M = 1000 * K;
    private static final long G = 1000 * M;

    public static final DecimalFormatSymbols DECIMAL = DecimalFormatSymbols.getInstance(Locale.US);

    private final DecimalFormat format;
    private final DecimalFormat exactFormat;

    /**
     * Whether to use <a href="https://en.wikipedia.org/wiki/Binary_prefix">binary prefixes</a>
     */
    private final boolean mebi;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public FileSizeFormatter(DecimalFormat format, DecimalFormat exactFormat, boolean mebi) {
        this.format = format== null ? new DecimalFormat("#") : format;
        this.exactFormat = exactFormat == null ? this.format : exactFormat;
        this.mebi = mebi;
    }


    public static final FileSizeFormatter DEFAULT = FileSizeFormatter.builder()
        .pattern("#.0")
        .exactPattern("#")
        .mebi(true)
        .build();


    public static final FileSizeFormatter SI = FileSizeFormatter.builder()
        .pattern("#.0")
        .mebi(false)
        .build();

    public String format(@Nullable Number numberOfBytes) {
        return format(numberOfBytes, true);
    }


    public String format(@Nullable Number numberOfBytes, boolean exact) {
        if (numberOfBytes == null) {
            return "? B";
        }
        if (mebi) {
            return formatMebi(numberOfBytes, exact);
        } else {
            return formatSI(numberOfBytes, exact);
        }
    }

    /**
     * Given a number of bytes, processed in a certain duration, format it as certain amount of bytes per second.
     *
     */
    public String formatSpeed(@Nullable Number numberOfBytes, Duration duration) {
        if (numberOfBytes == null || duration == null) {
            return format(null, false) + "/s";
        }
        if (duration.isZero()) {
            return "\u221E B/s";
        }
        Float perSecond = 1000f * numberOfBytes.floatValue() / duration.toMillis();
        return format(perSecond, false) + "/s";
    }



    public String formatSpeed(Number length, Instant start) {
        return formatSpeed(length, Duration.between(start, Instant.now()));
    }

    private String formatMebi(Number length, boolean exact) {
        long longValue = length.longValue();
        if (longValue > GiB) {
            return format.format(length.floatValue() / GiB) + " GiB";
        }
        if (longValue > MiB) {
            return format.format(length.floatValue() / MiB) + " MiB";
        }
        if (longValue > KiB) {
            return format.format( length.floatValue() / KiB) + " KiB";
        }
        return (exact ? exactFormat.format(length) : format.format(length)) + " B";
    }


    private String formatSI(Number length, boolean exact) {
        long longValue = length.longValue();

        if (longValue > G) {
            return format.format(length.floatValue() / G) + " GB";
        }
        if (longValue > M) {
            return format.format(length.floatValue() / M) + " MB";
        }
        if (longValue > K) {
            return format.format(length.floatValue() / K) + " KB";
        }
        return (exact ? exactFormat.format(length) : format.format(length)) + " B";
    }


    public static class Builder {
        {
            mebi = true;
        }
        private DecimalFormatSymbols symbols = DECIMAL;

        public Builder decimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
            this.symbols = decimalFormatSymbols == null ? DECIMAL : decimalFormatSymbols;
            if (Builder.this.format != null) {
                Builder.this.format.setDecimalFormatSymbols(decimalFormatSymbols);
            }
            return this;
        }

        public Builder decimalFormatSymbols(Locale locale) {
            return decimalFormatSymbols(new DecimalFormatSymbols(locale));
        }

        public Builder pattern(String pattern) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            decimalFormat.setDecimalFormatSymbols(symbols);
            return format(decimalFormat);
        }
        public Builder exactPattern(String pattern) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            decimalFormat.setDecimalFormatSymbols(symbols);
            return exactFormat(decimalFormat);
        }


    }

}

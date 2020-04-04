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
 *
 * E.g. {@code
 * FileSizeFormatter.DEFAULT.format(12345)
 * }
 *
 * For customizing a builder is available (Use {@link #builder()})
 *
 * Find more examples in {@link FileSizeFormatterTest}.
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

    /**
     * Whether to use <a href="https://en.wikipedia.org/wiki/Binary_prefix">binary prefixes</a>
     */
    private final boolean mebi;

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    public FileSizeFormatter(DecimalFormat format, boolean mebi) {
        this.format = format== null ? new DecimalFormat("#") : format;
        this.mebi = mebi;
    }


    public static final FileSizeFormatter DEFAULT = FileSizeFormatter.builder()
        .pattern("#.0")
        .mebi(true)
        .build();


    public static final FileSizeFormatter SI = FileSizeFormatter.builder()
        .pattern("#.0")
        .mebi(false)
        .build();

    public String format(@Nullable Number numberOfBytes) {
        if (numberOfBytes == null) {
            return "? B";
        }
        if (mebi) {
            return formatMebi(numberOfBytes.floatValue());
        } else {
            return formatSI(numberOfBytes.floatValue());
        }
    }

    /**
     * Given a number of bytes, processed in a certain duration, format it as certain amount of bytes per second.
     *
     */
    public String formatSpeed(@Nullable Number numberOfBytes, Duration duration) {
        if (numberOfBytes == null) {
            return format(numberOfBytes) + "/s";
        }
        Float perSecond = 1000f * numberOfBytes.floatValue() / duration.toMillis();
        return format(perSecond) + "/s";
    }



    public String formatSpeed(Number length, Instant start) {
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


    }

}

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

    /**
     * Creates a file-size formatter.
     *
     * @param format format used for scaled values
     * @param exactFormat format used for byte values when exact formatting is requested; defaults to {@code format}
     * @param mebi whether to use binary rather than SI prefixes
     */
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


    /**
     * Formats a number of bytes, using exact formatting for values below the selected prefix threshold.
     *
     * @param numberOfBytes number of bytes to format, or {@code null}
     * @return the formatted file size, or {@code "? B"} when the value is {@code null}
     */
    public String format(@Nullable Number numberOfBytes) {
        return format(numberOfBytes, true);
    }


    /**
     * Formats a number of bytes.
     *
     * @param numberOfBytes number of bytes to format, or {@code null}
     * @param exact whether values expressed in bytes use the exact format
     * @return the formatted file size, or {@code "? B"} when the value is {@code null}
     */
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
     * @param numberOfBytes number of processed bytes, or {@code null}
     * @param duration duration during which the bytes were processed, or {@code null}
     * @return the formatted transfer speed
     */
    public String formatSpeed(@Nullable Number numberOfBytes, Duration duration) {
        if (numberOfBytes == null || duration == null) {
            return format(null, false) + "/s";
        }
        if (duration.isZero()) {
            return "∞ B/s";
        }
        Float perSecond = 1000f * numberOfBytes.floatValue() / duration.toMillis();
        return format(perSecond, false) + "/s";
    }


    /**
     * Formats a transfer speed for bytes processed since a given instant.
     *
     * @param length number of processed bytes
     * @param start instant at which processing started
     * @return the formatted transfer speed
     */
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

    public FileSizeFormatter withPattern(String pattern) {
        return toBuilder().pattern(pattern).build();
    }

    public FileSizeFormatter withExtraDigit() {
        String p = pattern();
        if (p.contains(".")) {
            return toBuilder().pattern(p + "0").build();
        } else {
            return toBuilder().pattern(p + ".0").build();
        }
    }

    public String pattern() {
        return format.toPattern();
    }


    /**
     * Parses a file size expressed as a number optionally followed by a supported unit.
     *
     * <p>Supported units are {@code B}, {@code KB}, {@code MB}, {@code GB}, {@code KiB}, {@code MiB}, and
     * {@code GiB}; unit matching is case-insensitive.</p>
     *
     * @param string file size to parse
     * @return the corresponding number of bytes, rounded to the nearest whole byte
     * @throws IllegalArgumentException if the value is null, blank, or uses an unknown unit
     * @throws NumberFormatException if the numeric part is invalid
     */
    public static long parse(String string) {
        if (string == null || string.isBlank()) {
            throw new IllegalArgumentException("Cannot parse null or blank string");
        }
        string = string.trim();

        // Find where the numeric part ends (digits, dot, comma as decimal separator, underscore like Java literals)
        int i = 0;
        boolean foundSign = false;
        while (i < string.length()) {
            char c = string.charAt(i);
            if (c == '+' || c == '-') {
                if (foundSign) {
                    break; // Only one sign allowed, and it must be at the beginning
                }
                foundSign = true;
                i++;
                continue;
            }
            if (Character.isDigit(c) || c == '.' || c == ',' || c == '_') {
                i++;
            } else {
                break;
            }
        }

        String numberPart = string.substring(0, i).replace("_", "").replace(',', '.').trim();
        String unitPart = string.substring(i).trim().toLowerCase();

        double number = Double.parseDouble(numberPart);

        long multiplier = switch (unitPart) {
            case "kib" -> KiB;
            case "mib" -> MiB;
            case "gib" -> GiB;
            case "kb"  -> K;
            case "mb"  -> M;
            case "gb"  -> G;
            case "b", "" -> 1L;
            default -> throw new IllegalArgumentException("Unknown unit: '" + unitPart + "' in '" + string + "'");
        };

        return Math.round(number * multiplier);
    }


    /**
     * Builder for {@link FileSizeFormatter} instances.
     */
    public static class Builder {
        {
            mebi = true;
        }
        private DecimalFormatSymbols symbols = DECIMAL;

        /**
         * Sets the symbols used by formats created from a pattern.
         *
         * @param decimalFormatSymbols symbols to use, or {@link FileSizeFormatter#DECIMAL} when {@code null}
         * @return this builder
         */
        public Builder decimalFormatSymbols(DecimalFormatSymbols decimalFormatSymbols) {
            this.symbols = decimalFormatSymbols == null ? DECIMAL : decimalFormatSymbols;
            if (Builder.this.format != null) {
                Builder.this.format.setDecimalFormatSymbols(decimalFormatSymbols);
            }
            return this;
        }

        /**
         * Sets the locale from which symbols for formats created from a pattern are derived.
         *
         * @param locale locale to use
         * @return this builder
         */
        public Builder decimalFormatSymbols(Locale locale) {
            return decimalFormatSymbols(new DecimalFormatSymbols(locale));
        }

        /**
         * Sets the format pattern for scaled values.
         *
         * @param pattern decimal format pattern
         * @return this builder
         */
        public Builder pattern(String pattern) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            decimalFormat.setDecimalFormatSymbols(symbols);
            return format(decimalFormat);
        }

        /**
         * Sets the format pattern for byte values when exact formatting is requested.
         *
         * @param pattern decimal format pattern
         * @return this builder
         */
        public Builder exactPattern(String pattern) {
            DecimalFormat decimalFormat = new DecimalFormat(pattern);
            decimalFormat.setDecimalFormatSymbols(symbols);
            return exactFormat(decimalFormat);
        }


    }

}

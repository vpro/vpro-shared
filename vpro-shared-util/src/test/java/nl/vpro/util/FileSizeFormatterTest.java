package nl.vpro.util;

import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class FileSizeFormatterTest {

    @Test
    public void mebi() {
        FileSizeFormatter formatter = FileSizeFormatter.builder()
            .build();
        assertThat(formatter.format(1000L)).isEqualTo("1000 B");
        assertThat(formatter.format(221400200L)).isEqualTo("211 MiB");

    }


    @Test
    public void testDefault() {
        FileSizeFormatter formatter = FileSizeFormatter.DEFAULT;
        assertThat(formatter.format(1000L)).isEqualTo("1000.0 B");
        assertThat(formatter.format(221400200L)).isEqualTo("211.1 MiB");
    }


    @Test
    public void testDefaultSpeed() {
        FileSizeFormatter formatter = FileSizeFormatter.DEFAULT;
        assertThat(formatter.formatSpeed(221400200L, Duration.ofMillis(12344L))).isEqualTo("17.1 MiB/s");
    }

    @Test
    public void testNull() {
        FileSizeFormatter formatter = FileSizeFormatter.DEFAULT;
        assertThat(formatter.formatSpeed(null, Duration.ofMillis(12344L))).isEqualTo("? B/s");
    }


    @Test
    public void testDefaultChangedPrecision() {
        FileSizeFormatter formatter = FileSizeFormatter.DEFAULT.toBuilder().pattern("#.00").build();
        assertThat(formatter.format(1000L)).isEqualTo("1000.00 B");
        assertThat(formatter.format(221400200L)).isEqualTo("211.14 MiB");

    }


    @Test
    public void testNl() {
        FileSizeFormatter formatter = FileSizeFormatter.builder()
            .decimalFormatSymbols(Locale.forLanguageTag("nl"))
            .pattern("##.00")
            .mebi(false)
            .build();
        assertThat(formatter.format(221400200L)).isEqualTo("221,40 MB");

    }


    @Test
    public void si() {
        FileSizeFormatter formatter = FileSizeFormatter.builder()
            .mebi(false)
            .build();
        assertThat(formatter.format(1000L)).isEqualTo("1000 B");
        assertThat(formatter.format(221400200L)).isEqualTo("221 MB");

    }
}

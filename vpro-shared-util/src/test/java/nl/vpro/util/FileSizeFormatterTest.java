package nl.vpro.util;

import lombok.extern.log4j.Log4j2;
import net.jqwik.api.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
@Log4j2
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
        assertThat(formatter.format(1000L)).isEqualTo("1000 B");
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
        assertThat(formatter.formatSpeed(0, Duration.ofMillis(12344L))).isEqualTo(".0 B/s"); // ?
        assertThat(formatter.formatSpeed(1000, Duration.ofMillis(0))).isEqualTo("\u221E B/s");
        assertThat(formatter.formatSpeed(1000, (Duration) null)).isEqualTo("? B/s");

    }


    @Test
    public void testDefaultChangedPrecision() {
        FileSizeFormatter formatter = FileSizeFormatter.DEFAULT.toBuilder().pattern("#.00").build();
        assertThat(formatter.format(1000L)).isEqualTo("1000 B");
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

    @Test
    public void formatAndParse1025() {
        formatAndParse(1025);
    }

    @Property(tries = 20)
    public void formatAndParse(@ForAll("longs") long size) {
        for (FileSizeFormatter formatter : Arrays.asList(FileSizeFormatter.DEFAULT, FileSizeFormatter.SI)) {

            String formatted = formatter.format(size);
            long parsed = FileSizeFormatter.parse(formatted);
            log.info("size {} -> {} = {}", size, formatted, parsed);
            assertThat(parsed).isCloseTo(size, Percentage.withPercentage(10));
        }
    }

    @Provide()
    public Arbitrary<Long> longs() {
        return Arbitraries.longs().between(-10L * 1024 * 1024 * 1024, 10L * 1024 * 1024 * 1024);
    }

}

package nl.vpro.util;

import org.junit.Test;

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
        assertThat(formatter.format(1000L)).isEqualTo("1000 B");
        assertThat(formatter.format(221400200L)).isEqualTo("211.1 MiB");

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

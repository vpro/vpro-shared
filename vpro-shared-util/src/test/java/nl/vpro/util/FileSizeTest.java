package nl.vpro.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class FileSizeTest {

    @Test
    public void format() {
        assertThat(FileSize.format(1000L)).isEqualTo("1000 B");
        assertThat(FileSize.format(221400200L)).isEqualTo("211 MiB");

    }
}

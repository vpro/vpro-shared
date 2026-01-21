package nl.vpro.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class URLUtilsTest {

    @Test
    public void hidePassword() {
        assertThat( URLUtils.hidePassword("https://user:password@example.com/path?query=1")).isEqualTo("https://user:*****@example.com/path?query=1");
    }

}

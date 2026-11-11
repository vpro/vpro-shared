package nl.vpro.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class URLUtilsTest {

    @Test
    public void hidePassword() {
        assertThat( URLUtils.hidePassword("https://user:password@example.com/path?query=1")).isEqualTo("https://user:*****@example.com/path?query=1");
    }

    @Test
    public void addPassword() {
        assertThat( URLUtils.addAuthentication("https://example.com/path?query=1", "user:password")).isEqualTo("https://user:password@example.com/path?query=1");
    }

    @Test
    public void addQueryParameter() {
        assertThat(URLUtils.addQueryParameter("https://example.com/path", "a", "1")).isEqualTo("https://example.com/path?a=1");
        assertThat(URLUtils.addQueryParameter("https://example.com/path?query=1", "a", "1")).isEqualTo("https://example.com/path?query=1&a=1");
        assertThat(URLUtils.addQueryParameter("https://example.com/path?", "a", "1")).isEqualTo("https://example.com/path?a=1");
        assertThat(URLUtils.addQueryParameter("https://example.com/path?query=1&", "a", "1")).isEqualTo("https://example.com/path?query=1&a=1");
        assertThat(URLUtils.addQueryParameter("https://example.com/path?query=1#frag", "a", "1")).isEqualTo("https://example.com/path?query=1&a=1#frag");
        assertThat(URLUtils.addQueryParameter("https://example.com/path", "q", "a b&c=d/é")).isEqualTo("https://example.com/path?q=a%20b%26c%3Dd%2F%C3%A9");
        assertThat(URLUtils.addQueryParameter("https://example.com/path", "flag", null)).isEqualTo("https://example.com/path?flag");
        assertThat(URLUtils.addQueryParameter(null, "a", "1")).isNull();
    }

}

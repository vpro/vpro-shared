package nl.vpro.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
class UrlProviderTest {
    @Test
    void test() {
        UrlProvider provider = new UrlProvider("a", 80);
        assertEquals("a", provider.getHost());
        assertEquals(80, provider.getPort());

        provider.setPath("bla"); // uri?

        assertEquals("bla", provider.getPath());

        assertEquals("http://a/bla", provider.getUrl());

        provider = new UrlProvider("a", 81);
        assertEquals("http://a:81/", provider.getUrl());

    }

    @Test
    void url() {
        UrlProvider provider = UrlProvider.fromUrl("http://a:81/b");
        assertEquals("http://a:81/b", provider.getUrl());

    }

}

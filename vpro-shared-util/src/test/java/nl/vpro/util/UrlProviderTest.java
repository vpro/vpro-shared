package nl.vpro.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class UrlProviderTest {
    @Test
    public void test() {
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
    public void testUrl() {
        UrlProvider provider = UrlProvider.fromUrl("http://a:81/b");
        assertEquals("http://a:81/b", provider.getUrl());

    }

}

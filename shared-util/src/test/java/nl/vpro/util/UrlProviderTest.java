package nl.vpro.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class UrlProviderTest {
    @Test
    public void test() throws Exception {
        UrlProvider provider = new UrlProvider("a", 80);
        assertEquals("a", provider.getHost());
        assertEquals(80, provider.getPort());

        provider.setUri("bla"); // uri?

        assertEquals("bla", provider.getUri());

        assertEquals("http://a/bla", provider.getUrl());

        provider = new UrlProvider("a", 81);
        assertEquals("http://a:81/", provider.getUrl());

    }

}

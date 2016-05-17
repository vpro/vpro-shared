package nl.vpro.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import static nl.vpro.util.URLResource.PROPERTIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Michiel Meeuwissen
 * @since 0.37
 */
public class URLResourceTest {


    @Test
    public void broadcasters() {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create("http://poms.omroep.nl/broadcasters/"));
        assertTrue(broadcasters.get().size() > 0);
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());
        broadcasters.get();
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(1, broadcasters.getNotCheckedCount());
        System.out.println(broadcasters.get());
    }


    @Test
    public void broadcastersFromClassPath() throws InterruptedException {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create("classpath:/broadcasters.properties"));
        broadcasters.setMinAge(Duration.ofMillis(100));
        assertTrue(broadcasters.get().size() > 0);
        assertEquals(0, broadcasters.getNotCheckedCount());
        assertEquals(1, broadcasters.getCheckedCount());

        broadcasters.get();
        assertEquals(1, broadcasters.getNotCheckedCount());
        Thread.sleep(150);
        broadcasters.get();
        assertEquals(2, broadcasters.getCheckedCount());
        assertEquals(0, broadcasters.getChangesCount());
        assertEquals(1, broadcasters.getNotCheckedCount());
        broadcasters.get();
        assertEquals(0, broadcasters.getChangesCount());
        assertEquals(2, broadcasters.getNotCheckedCount());

    }


    @Test
    public void broadcastersFromClassPathMap() {
        URLResource<Map<String, String>> broadcasters = URLResource.map(URI.create("classpath:/broadcasters.properties"));
        assertTrue(broadcasters.get().size() > 0);
        assertEquals(0, broadcasters.getNotCheckedCount());
        broadcasters.get();
        assertEquals(1, broadcasters.getNotCheckedCount());
    }

    @Test
    public void broadcasters503() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(503, 200, 500, 200);
        when(connection.getInputStream()).thenAnswer((Answer<InputStream>) invocation -> new ByteArrayInputStream("VPRO=VPRO".getBytes()));

        URLResource<Properties> broadcasters = new URLResource<Properties>(URI.create("http://poms.omroep.nl/broadcasters/"), PROPERTIES, new Properties()) {
            @Override
            public URLConnection openConnection() {
                return connection;
            }
        }.setMinAge(Duration.ZERO).setErrorCache(Duration.ZERO);



        assertTrue(broadcasters.get().size() == 0);
        assertEquals(0, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());
        assertEquals(1, broadcasters.getErrorCount());
        Properties props = broadcasters.get();
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());
        assertEquals(1, broadcasters.getErrorCount());
        assertEquals(1, props.size());
        props = broadcasters.get();
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());
        assertEquals(2, broadcasters.getErrorCount());
        assertEquals(1, props.size());
        props = broadcasters.get();
        assertEquals(2, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());
        assertEquals(2, broadcasters.getErrorCount());
        assertEquals(1, props.size());
    }
}

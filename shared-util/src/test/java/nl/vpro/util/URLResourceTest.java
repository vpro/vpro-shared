package nl.vpro.util;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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
}

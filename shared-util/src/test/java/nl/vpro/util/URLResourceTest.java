package nl.vpro.util;

import java.net.URI;
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
    }


    @Test
    public void broadcastersFromClassPath() {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create("classpath:/broadcasters.properties"));
        assertTrue(broadcasters.get().size() > 0);
        assertEquals(0, broadcasters.getNotCheckedCount());
        broadcasters.get();
        assertEquals(1, broadcasters.getNotCheckedCount());
    }

}
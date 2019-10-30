package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.*;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static nl.vpro.util.URLResource.PROPERTIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Michiel Meeuwissen
 * @since 0.37
 */
@Slf4j
public class URLResourceTest {

    @ClassRule
    public static WireMockClassRule wireMockClassRule = new WireMockClassRule(wireMockConfig().dynamicPort());

    @Rule
    public WireMockClassRule wireMock = wireMockClassRule;


    @Before
    public  void init() throws IOException {
        stubFor(get(urlEqualTo("/broadcasters"))
            .willReturn(
                aResponse()
                    .withBody(IOUtils.resourceToByteArray("/broadcasters.properties"))
                    .withHeader("Cache-Control", "public, max-age: 3600")
                    .withHeader("Last-Modified", "Wed, 24 Apr 2019 05:55:21 GMT")
            ));
        stubFor(get(urlEqualTo("/broadcasters"))
            .withHeader("If-Modified-Since", equalTo("Wed, 24 Apr 2019 05:55:21 GMT"))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.SC_NOT_MODIFIED)
                ));
        stubFor(get(urlEqualTo("/redirect"))
            .willReturn(
                aResponse()
                    .withHeader("Location", "/broadcasters")
                    .withStatus(HttpStatus.SC_MOVED_PERMANENTLY)
                ));
    }



    @Test
    public void broadcasters() throws InterruptedException {




        URLResource<Properties> broadcasters =
            URLResource.properties(URI.create("http://localhost:" + wireMock.port() + "/broadcasters"));

        assertTrue(broadcasters.get().size() > 0);
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());

        broadcasters.get();

        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(1, broadcasters.getNotCheckedCount());

        broadcasters.setMinAge(Duration.ofMillis(1));
        Thread.sleep(2);
        broadcasters.expire();
        log.info("{}", broadcasters.get().size());
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(1, broadcasters.getNotModifiedCount());
        assertEquals(1, broadcasters.getNotCheckedCount());

        broadcasters.setMinAge(Duration.ofMinutes(5));

        broadcasters.get();
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(1, broadcasters.getNotModifiedCount());
        assertEquals(2, broadcasters.getNotCheckedCount());

        System.out.println(broadcasters.get());
        Thread.sleep(2000L);
        broadcasters.setMinAge(Duration.ofSeconds(1));
        broadcasters.setMaxAge(Duration.ofSeconds(1));
        System.out.println(broadcasters.get());

    }

    @Test
    public void broadcastersRedirect() {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create("http://localhost:" + wireMock.port() + "/redirect"));

        assertTrue(broadcasters.get().size() > 0);

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
        when(connection.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream("VPRO=VPRO".getBytes()));

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

    @Test
    @Ignore
    public void broadcastersReal() {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create("http://localhost:8071/broadcasters/"));
        broadcasters.setMinAge(Duration.ZERO);
        assertTrue(broadcasters.get().size() > 0);
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(0, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());
        broadcasters.get();
        assertEquals(1, broadcasters.getChangesCount());
        assertEquals(1, broadcasters.getNotModifiedCount());
        assertEquals(0, broadcasters.getNotCheckedCount());
        System.out.println(broadcasters.get());
    }
}

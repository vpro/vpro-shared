package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static nl.vpro.util.URLResource.PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Michiel Meeuwissen
 * @since 0.37
 */
@Slf4j
@WireMockTest
public class URLResourceTest {

    @BeforeEach
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
    public void broadcasters(WireMockRuntimeInfo info) throws InterruptedException {

        URLResource<Properties> broadcasters =
            URLResource.properties(URI.create(info.getHttpBaseUrl() + "/broadcasters"));

        assertThat(broadcasters.get()).isNotEmpty();
        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);

        broadcasters.get();

        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(1);

        broadcasters.setMinAge(Duration.ofMillis(1));
        Thread.sleep(2);
        broadcasters.expire();
        log.info("{}", broadcasters.get().size());
        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(1);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(1);

        broadcasters.setMinAge(Duration.ofMinutes(5));

        broadcasters.get();
        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(1);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(2);

        System.out.println(broadcasters.get());
        Thread.sleep(2000L);
        broadcasters.setMinAge(Duration.ofSeconds(1));
        broadcasters.setMaxAge(Duration.ofSeconds(1));
        System.out.println(broadcasters.get());

    }

    @Test
    public void broadcastersRedirect(WireMockRuntimeInfo runtimeInfo) {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create(runtimeInfo.getHttpBaseUrl() + "/redirect"));

        assertTrue(broadcasters.get().size() > 0);

    }


    @Test
    public void broadcastersFromClassPath() throws InterruptedException {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create("classpath:/broadcasters.properties"));
        broadcasters.setMinAge(Duration.ofMillis(100));
        assertTrue(broadcasters.get().size() > 0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        assertThat(broadcasters.getCheckedCount()).isEqualTo(1);

        broadcasters.get();
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(1);
        Thread.sleep(150);
        broadcasters.get();
        assertThat(broadcasters.getCheckedCount()).isEqualTo(2);
        assertThat(broadcasters.getChangesCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(1);
        broadcasters.get();
        assertThat(broadcasters.getChangesCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(2);

    }


    @Test
    public void broadcastersFromClassPathMap() {
        URLResource<Map<String, String>> broadcasters = URLResource.map(URI.create("classpath:/broadcasters.properties"));
        assertTrue(broadcasters.get().size() > 0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        broadcasters.get();
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(1);
    }

    @Test
    public void broadcasters503() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(503, 200, 500, 200);
        when(connection.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream("VPRO=VPRO".getBytes()));

        URLResource<Properties> broadcasters = new URLResource<Properties>(URI.create("https://poms.omroep.nl/broadcasters/"), PROPERTIES, new Properties()) {
            @Override
            public URLConnection openConnection() {
                return connection;
            }
        }.setMinAge(Duration.ZERO).setErrorCache(Duration.ZERO);



        assertThat(broadcasters.get()).hasSize(0);
        assertThat(broadcasters.getChangesCount()).isEqualTo(0);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        assertThat(broadcasters.getErrorCount()).isEqualTo(1);
        Properties props = broadcasters.get();
        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        assertThat(broadcasters.getErrorCount()).isEqualTo(1);
        assertThat((long) props.size()).isEqualTo(1);
        props = broadcasters.get();
        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        assertThat(broadcasters.getErrorCount()).isEqualTo(2);
        assertThat((long) props.size()).isEqualTo(1);
        props = broadcasters.get();
        assertThat(broadcasters.getChangesCount()).isEqualTo(2);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        assertThat(broadcasters.getErrorCount()).isEqualTo(2);
        assertThat((long) props.size()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-acc", "-test"})
    public void broadcastersReal(String env) {
        URLResource<Properties> broadcasters = URLResource.properties(URI.create(String.format("https://poms%s.omroep.nl/broadcasters/", env)));
        broadcasters.setMinAge(Duration.ZERO);
        assertTrue(broadcasters.get().size() > 0);
        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        broadcasters.get();
        assertThat(broadcasters.getChangesCount()).isEqualTo(1);
        assertThat(broadcasters.getNotModifiedCount()).isEqualTo(0);
        assertThat(broadcasters.getNotCheckedCount()).isEqualTo(0);
        System.out.println(broadcasters.get());
    }

}

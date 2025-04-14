package nl.vpro.util;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;


@WireMockTest
class HttpConnectionUtilsTest {

    @BeforeEach
    public  void init() {
        stubFor(head(urlEqualTo("/legal"))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.SC_UNAVAILABLE_FOR_LEGAL_REASONS)
            ));
        stubFor(head(urlEqualTo("/ok"))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.SC_OK)
                    .withHeader("Content-Type", "image/jpeg")
                    .withHeader("Content-Length", "54321")
            ));
        stubFor(head(urlEqualTo("/error"))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.SC_SERVER_ERROR)
                    .withHeader("Content-Type", "text/plain")
                    .withBody("Something went wrong")
            ));


    }


    @Test
    public void ok(WireMockRuntimeInfo info) {

        assertThat(HttpConnectionUtils.getByteSize(info.getHttpBaseUrl() + "/ok")).isEqualTo(54321L);

    }

    @Test
    public void legallyDenied(WireMockRuntimeInfo info) {

        assertThat(HttpConnectionUtils.getByteSize(info.getHttpBaseUrl() + "/legal")).isNull();


        assertThat(HttpConnectionUtils.getOptionalByteSize(
            info.getHttpBaseUrl() + "/legal",
            (response, e) -> {
                assertThat(response.statusCode()).isEqualTo(451);

            })).isEmpty();

    }



    @Test
    public void error(WireMockRuntimeInfo info) {

        assertThat(HttpConnectionUtils.getOptionalByteSize(info.getHttpBaseUrl() + "/error")).isEmpty();


        assertThat(HttpConnectionUtils.getOptionalByteSize(
            info.getHttpBaseUrl() + "/error",
            (response, e) -> {
                assertThat(response.statusCode()).isEqualTo(500);

            })).isEmpty();

    }


    @Test
    public void connection(WireMockRuntimeInfo info) {
        assertThat(HttpConnectionUtils.getByteSize("http://localhost:9999")).isNull();

    }


    @Test
    public void protocol(WireMockRuntimeInfo info) {
        assertThat(HttpConnectionUtils.getByteSize("mailto:foo@mail.com")).isNull();

    }



}

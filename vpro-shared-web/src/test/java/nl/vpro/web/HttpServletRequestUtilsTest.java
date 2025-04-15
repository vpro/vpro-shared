package nl.vpro.web;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class HttpServletRequestUtilsTest {

    @Test
    public void getContextURL() {
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getServerName()).thenReturn("bla");
        when(mock.getServerPort()).thenReturn(443);
        when(mock.getScheme()).thenReturn("https");
        when(mock.getContextPath()).thenReturn("");

        assertThat(HttpServletRequestUtils.getContextURL(mock)).isEqualToIgnoringCase("https://bla");
    }

    @Test
    public void getContextURLBehindProxy() {
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);

        when(mock.getHeader("X-Forwarded-Host")).thenReturn("foo");
        when(mock.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(mock.getHeader("X-Forwarded-Port")).thenReturn("443");

        when(mock.getServerName()).thenReturn("bla");
        when(mock.getServerPort()).thenReturn(80);
        when(mock.getScheme()).thenReturn("http");
        when(mock.getContextPath()).thenReturn("/context");

        assertThat(HttpServletRequestUtils.getContextURL(mock)).isEqualToIgnoringCase("https://foo/context");
    }

    @Test
    public void getContextURL2() {
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getServerName()).thenReturn("foo");
        when(mock.getServerPort()).thenReturn(8080);
        when(mock.getScheme()).thenReturn("http");
        when(mock.getContextPath()).thenReturn("/bar");

        assertThat(HttpServletRequestUtils.getContextURL(mock)).isEqualToIgnoringCase("http://foo:8080/bar");
    }

}

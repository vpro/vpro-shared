package nl.vpro.web;

import javax.servlet.http.HttpServletRequest;

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
    public void getContextURL2() {
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getServerName()).thenReturn("foo");
        when(mock.getServerPort()).thenReturn(8080);
        when(mock.getScheme()).thenReturn("http");
        when(mock.getContextPath()).thenReturn("/bar");

        assertThat(HttpServletRequestUtils.getContextURL(mock)).isEqualToIgnoringCase("http://foo:8080/bar");
    }

}

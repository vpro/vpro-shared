package nl.vpro.api.client.resteasy;

import java.util.ArrayList;
import java.util.Locale;

import javax.ws.rs.client.ClientRequestContext;

import org.jboss.resteasy.core.Headers;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class AcceptLanguageRequestFilterTest {
    AcceptLanguageRequestFilter instance = new AcceptLanguageRequestFilter(Locale.ENGLISH, new Locale("nl", "NL"));
    ClientRequestContext context = mock(ClientRequestContext.class);
    Headers<Object> headers = new Headers<>();

    @Before
    public void setup() {
        reset(context);
        when(context.getHeaders()).thenReturn(headers);
        headers.clear();
    }

    @Test
    public void filterWithoutExisting() throws Exception {
        instance.filter(context);
        assertThat(headers.getFirst("Accept-Language")).isEqualTo("en, nl_NL");
    }

    @Test
    public void filterWithemptyExisting() throws Exception {
        headers.put("Accept-Language", new ArrayList<>());
        instance.filter(context);
        assertThat(headers.getFirst("Accept-Language")).isEqualTo("en, nl_NL");
    }

    @Test
    public void filterWithExisting() throws Exception {
        headers.putSingle("Accept-Language", "de, nl");
        instance.filter(context);
        assertThat(headers.getFirst("Accept-Language")).isEqualTo("en, nl_NL");
    }

}

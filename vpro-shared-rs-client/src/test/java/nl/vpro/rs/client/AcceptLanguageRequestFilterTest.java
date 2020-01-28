package nl.vpro.rs.client;

import java.util.ArrayList;
import java.util.Locale;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class AcceptLanguageRequestFilterTest {
    AcceptLanguageRequestFilter instance = new AcceptLanguageRequestFilter(Locale.ENGLISH, new Locale("nl", "NL"));
    ClientRequestContext context = mock(ClientRequestContext.class);
    MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();

    @Before
    public void setup() {
        reset(context);
        when(context.getHeaders()).thenReturn(headers);
        headers.clear();
    }

    @Test
    public void filterWithoutExisting() {
        instance.filter(context);
        assertThat(headers.getFirst("Accept-Language")).isEqualTo("en, nl_NL");
    }

    @Test
    public void filterWithemptyExisting() {
        headers.put("Accept-Language", new ArrayList<>());
        instance.filter(context);
        assertThat(headers.getFirst("Accept-Language")).isEqualTo("en, nl_NL");
    }

    @Test
    public void filterWithExisting() {
        headers.putSingle("Accept-Language", "de, nl");
        instance.filter(context);
        assertThat(headers.getFirst("Accept-Language")).isEqualTo("en, nl_NL");
    }

}

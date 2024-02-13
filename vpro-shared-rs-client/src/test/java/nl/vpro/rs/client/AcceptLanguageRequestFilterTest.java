package nl.vpro.rs.client;

import java.util.ArrayList;
import java.util.Locale;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
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

package nl.vpro.rs.client;

import java.util.ArrayList;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.core.Headers;
import org.junit.Before;
import org.junit.Test;

import nl.vpro.rs.client.AcceptRequestFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class AcceptRequestFilterTest {

    AcceptRequestFilter instance = new AcceptRequestFilter(MediaType.APPLICATION_JSON_TYPE);
    ClientRequestContext context = mock(ClientRequestContext.class);
    Headers<Object> headers = new Headers<>();
    @Before
    public void setup() {
        reset(context);
        when(context.getHeaders()).thenReturn(headers);
        headers.clear();
    }


    @Test
    public void filterWithoutExisting() {
        instance.filter(context);
        assertThat(headers.getFirst("Accept")).isEqualTo("application/json");

    }

    @Test
    public void filterWithemptyExisting() {
        headers.put("Accept", new ArrayList<>());
        instance.filter(context);
        assertThat(headers.getFirst("Accept")).isEqualTo("application/json");

    }

    @Test
    public void filterWithExisting() {
        headers.putSingle("Accept", "application/xml, application/json;charset=UTF-8");
        instance.filter(context);
        assertThat(headers.getFirst("Accept")).isEqualTo("application/json;charset=UTF-8, application/xml;q=0.5");

    }

}

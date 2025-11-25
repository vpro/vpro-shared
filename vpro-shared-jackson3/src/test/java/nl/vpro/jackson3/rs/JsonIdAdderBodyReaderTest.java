package nl.vpro.jackson3.rs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Providers;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.jackson3.rs.JacksonContextResolver;
import nl.vpro.jackson3.rs.JsonIdAdderBodyReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since 2.7
 */
public class JsonIdAdderBodyReaderTest {

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "objectType"
    )
    @JsonSubTypes(
        {
            @JsonSubTypes.Type(value = A.class, name = "a"),
            @JsonSubTypes.Type(value = B.class, name = "b")
        })
    public interface  BaseI {

    }

    public static abstract class Base implements BaseI {

    }

    @JsonTypeName("a")
    public static class A extends Base {

    }
    @JsonTypeName("b")
    public static class B extends Base {

    }
    public static class C {

    }
    final JsonIdAdderBodyReader idAdderInterceptor = new JsonIdAdderBodyReader();
    {
        idAdderInterceptor.providers = Mockito.mock(Providers.class);
        when(idAdderInterceptor.providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE)).thenReturn(new JacksonContextResolver());
    }

    @Test
    public void testA() throws IOException {

        Object o = idAdderInterceptor.readFrom(Object.class,
            A.class,
            null,
            MediaType.APPLICATION_JSON_TYPE, null, new ByteArrayInputStream("{}".getBytes()));
        assertThat(o).isInstanceOf(A.class);
    }



    @Test
    public void testBase() throws IOException {

        Object o = idAdderInterceptor.readFrom(Object.class,
            Base.class,
            null,
            MediaType.APPLICATION_JSON_TYPE, null, new ByteArrayInputStream("{'objectType': 'a'}".getBytes()));
        assertThat(o).isInstanceOf(A.class);
    }
    @Test
    public void testC() throws IOException {

        Object o = idAdderInterceptor.readFrom(Object.class,
            C.class,
            null,
            MediaType.APPLICATION_JSON_TYPE, null, new ByteArrayInputStream("{}".getBytes()));
        assertThat(o).isInstanceOf(C.class);
    }

}

package nl.vpro.jackson2.rs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.7
 */
public class JsonIdAdderReaderTest {

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
    JsonIdAdderBodyReader idAdderInterceptor = new JsonIdAdderBodyReader();

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

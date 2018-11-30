package nl.vpro.jackson2;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public class LenientBooleanDeserializerTest {

    public static class A {
        @JsonDeserialize(using = LenientBooleanDeserializer.class)
        Boolean bool;
    }
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void deserialize() throws IOException {

        assertThat(mapper.readValue("{\"bool\": 1}", A.class).bool).isTrue();
        assertThat(mapper.readValue("{\"bool\": true}", A.class).bool).isTrue();
        assertThat(mapper.readValue("{\"bool\": \"1\"}", A.class).bool).isTrue();

        assertThat(mapper.readValue("{\"bool\": 0}", A.class).bool).isFalse();
        assertThat(mapper.readValue("{\"bool\": false}", A.class).bool).isFalse();
        assertThat(mapper.readValue("{\"bool\": \"0\"}", A.class).bool).isFalse();

    }

    @Test
    public void deserializeNull() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.readValue("{\"bool\": null}", A.class).bool).isNull();



    }
}

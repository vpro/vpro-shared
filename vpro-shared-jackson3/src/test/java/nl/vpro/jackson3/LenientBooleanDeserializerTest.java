package nl.vpro.jackson3;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public class LenientBooleanDeserializerTest {

    public static class A {
        @JsonDeserialize(using = LenientBooleanDeserializer.class)
        Boolean bool;

        @JsonDeserialize(using = LenientBooleanDeserializer.class)
        boolean b;

    }
    JsonMapper mapper = new JsonMapper();

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
        JsonMapper mapper = new JsonMapper();
        assertThat(mapper.readValue("{\"bool\": null}", A.class).bool).isNull();
        assertThat(mapper.readValue("{\"bool\": null}", A.class).b).isFalse();
        assertThat(mapper.readValue("{\"b\": \"x\"}", A.class).b).isFalse();




    }
}

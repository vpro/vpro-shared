package nl.vpro.jackson2;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.bind.annotation.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2
public class Jackson2MapperTest {

    public enum EnumValues {
        a,
        b
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @XmlRootElement
    public static class A {
        @XmlElement
        int integer;

        @XmlAttribute
        EnumValues enumValue;

        @XmlElement
        Optional<Integer> optional;
    }

    @Test
    public void read() throws IOException {
        String example = "/* leading comments */\n{'integer': 2 /* ignore comments */, 'optional': 3}";
        A a = Jackson2Mapper.getInstance().readValue(example, A.class);
        assertThat(a.integer).isEqualTo(2);
        assertThat(a.optional).isPresent();
        assertThat(a.optional.get()).isEqualTo(3);

        Jackson2Mapper.getLenientInstance().readerFor(A.class).readValue(example.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void readIntFromString() throws IOException {
        A a = Jackson2Mapper.getInstance().readValue("{'integer': '2'}", A.class);
        assertThat(a.integer).isEqualTo(2);
    }

    @Test
    public void readEnumValue() throws IOException {
        A a = Jackson2Mapper.getInstance().readValue("{'enumValue': 'a'}", A.class);
        assertThat(a.enumValue).isEqualTo(EnumValues.a);
    }

    @Test
    public void readUnknownEnumValue() throws IOException {
        assertThatThrownBy(() -> {
            A a = Jackson2Mapper.getInstance().readValue("{'enumValue': 'c'}", A.class);
            assertThat(a.enumValue).isNull();
        }).isInstanceOf(InvalidFormatException.class);
    }

    @Test
    public void readUnknownEnumValueLenient() throws IOException {
        A a = Jackson2Mapper.getLenientInstance().readValue("{'enumValue': 'c'}", A.class);
        assertThat(a.enumValue).isNull();
    }

    @Test
    public void write() throws JsonProcessingException {
        A a = new A();
        a.integer = 2;
        a.optional = Optional.of(3);
        assertThat(Jackson2Mapper.getInstance().writeValueAsString(a)).isEqualTo("{\"integer\":2,\"optional\":3}");
    }

    @Test
    public void writeWithEmptyOptional() throws JsonProcessingException {
        A a = new A();
        a.integer = 2;
        a.optional = Optional.empty();
        assertThat(Jackson2Mapper.getInstance().writeValueAsString(a)).isEqualTo("{\"integer\":2}");
    }

    @Test
    public void unmodifiable() {
        Jackson2Mapper mapper = Jackson2Mapper.getInstance();
        log.info("{}", mapper.getSerializationConfig().getActiveView());
        SerializationConfig serializationConfig = mapper.getSerializationConfig().withView(Views.Javascript.class);
        mapper.setConfig(serializationConfig);
        log.info("{}", mapper.getSerializationConfig().getActiveView());
    }

}

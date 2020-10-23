package nl.vpro.jackson2;

import java.io.IOException;
import java.util.Optional;

import javax.xml.bind.annotation.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        A a = Jackson2Mapper.getInstance().readValue("/* leading comments */\n{'integer': 2 /* ignore comments */, 'optional': 3}", A.class);
        assertThat(a.integer).isEqualTo(2);
        assertThat(a.optional).isPresent();
        assertThat(a.optional.get()).isEqualTo(3);

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
}

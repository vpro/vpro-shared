package nl.vpro.jackson3;

import lombok.extern.log4j.Log4j2;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.xml.bind.annotation.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Log4j2
public class Jackson3MapperTest {

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
    public void read() {
        String example = "/* leading comments */\n{'integer': 2 /* ignore comments */, 'optional': 3}";
        A a = Jackson3Mapper.INSTANCE.readerFor(A.class).readValue(example);
        assertThat(a.integer).isEqualTo(2);
        assertThat(a.optional).isPresent();
        assertThat(a.optional.get()).isEqualTo(3);

        Jackson3Mapper.LENIENT.readerFor(A.class).readValue(example.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void readIntFromString() {
        A a = Jackson3Mapper.INSTANCE.readerFor(A.class).readValue("{'integer': '2'}");
        assertThat(a.integer).isEqualTo(2);
    }

    @Test
    public void readEnumValue() {
        A a = Jackson3Mapper.INSTANCE.readerFor(A.class).readValue("{'enumValue': 'a'}");
        assertThat(a.enumValue).isEqualTo(EnumValues.a);
    }

    @Test
    public void readUnknownEnumValue() {
        assertThatThrownBy(() -> {
            A a = Jackson3Mapper.INSTANCE.readerFor(A.class).readValue("{'enumValue': 'c'}");
        }).isInstanceOf(InvalidFormatException.class);
    }

    @Test
    public void readUnknownEnumValueLenient() {
        A a = Jackson3Mapper.LENIENT.readerFor(A.class).readValue("{'enumValue': 'c'}");
        assertThat(a.enumValue).isNull();
    }

    @Test
    public void write() throws JacksonException {
        A a = new A();
        a.integer = 2;
        a.optional = Optional.of(3);
        assertThat(Jackson3Mapper.INSTANCE.writer().writeValueAsString(a)).isEqualTo("{\"integer\":2,\"optional\":3}");
    }

    @Test
    public void writeWithEmptyOptional() throws JacksonException {
        A a = new A();
        a.integer = 2;
        a.optional = Optional.empty();
        assertThat(Jackson3Mapper.INSTANCE.writer().writeValueAsString(a)).isEqualTo("{\"integer\":2}");
    }

}

package nl.vpro.jackson2;

import java.io.IOException;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import static org.assertj.core.api.Assertions.assertThat;

public class Jackson2MapperTest {

    public enum EnumValues {
        a,
        b;
    }

    @XmlRootElement
    public static class A {
        @XmlElement
        int integer;

        @XmlAttribute
        EnumValues enumValue;

    }
    @Test
    public void read() throws IOException {
        A a = Jackson2Mapper.getInstance().readValue("{'integer': 2}", A.class);
        assertThat(a.integer).isEqualTo(2);
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

    @Test(expected = InvalidFormatException.class)
    public void readUnknownEnumValue() throws IOException {
        A a = Jackson2Mapper.getInstance().readValue("{'enumValue': 'c'}", A.class);
        assertThat(a.enumValue).isNull();
    }

    @Test
    public void readUnknownEnumValueLenient() throws IOException {
        A a = Jackson2Mapper.getLenientInstance().readValue("{'enumValue': 'c'}", A.class);
        assertThat(a.enumValue).isNull();
    }
}

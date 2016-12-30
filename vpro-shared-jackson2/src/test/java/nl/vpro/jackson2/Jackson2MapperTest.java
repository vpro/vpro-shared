package nl.vpro.jackson2;

import java.io.IOException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Jackson2MapperTest {

    @XmlRootElement
    public static class A {
        @XmlElement
        int integer;
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


}

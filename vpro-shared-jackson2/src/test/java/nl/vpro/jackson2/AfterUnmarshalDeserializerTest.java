package nl.vpro.jackson2;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.StdConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Michiel Meeuwissen
 * @since 2.16
 */
@Slf4j
class AfterUnmarshalDeserializerTest {

    static int instances = 0;

    /**
     * An example of an object of which we want to make jackson call {@link #afterUnmarshal(Unmarshaller, Object)}
     *
     * Since jackson
     */
    @JsonDeserialize(using = AfterUnmarshalDeserializer.class)
    @XmlAccessorType(XmlAccessType.NONE)
    static class A {

        private final int instance = instances++;

        private boolean unmarshalled = false;

        Object parent;

        void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            unmarshalled = true;
            this.parent = parent;
            log.info("Calling afterUnmarshal on this {} {}", this, parent);
        }

        public String toString () {
            return "A:" + instance;
        }

    }

    @JsonDeserialize(using = AfterUnmarshalDeserializer.class)
    static class AWithContext {

        private final int instance = instances++;

        Object parent;

        void afterUnmarshal(DeserializationContext context, Object parent) {
            this.parent = parent;
            log.info("Calling afterUnmarshal on this {} {}", this, parent);
        }

        public String toString () {
            return "AWithContext:" + instance;
        }

    }

    /**
     * An example of an object of which we want to make jackson call {@link #afterUnmarshal(Object)}
     */
    @JsonDeserialize(using = AfterUnmarshalDeserializer.class, converter = AConverter.class)
    static class ASimple {

        private final int instance = instances++;

        @JsonIgnore
        Object parent;
        @JsonIgnore
        boolean converted = false;

        void afterUnmarshal(Object parent) {
            this.parent = parent;
            log.info("Calling afterUnmarshal on this {} {}", this, parent);
        }

        public String toString () {
            return "ASimple:" + instance;
        }

    }

    static class AConverter extends StdConverter<ASimple, ASimple> {

        @Override
        public ASimple convert(ASimple value) {
            value.converted = true;
            return value;
        }
    }

    @JsonDeserialize(using = AfterUnmarshalDeserializer.class)
    static class ErrorneousA {


    }

    @XmlRootElement
    static class B {
        private final int instance = instances++;

        @XmlElement
        A a;

        public String toString () {
            return "B:" + instance;
        }
    }

    @XmlRootElement
    static class C {
        private final int instance = instances++;

        @XmlElement
        List<A> as;

        public String toString () {
            return "C:" + instance;
        }
    }

    static class D {
        private final int instance = instances++;


        @JsonProperty
        @ToString.Exclude
        A a;

        @JsonProperty
        List<A> moreAs;

        public String toString () {
            return "D:" + instance;
        }
    }


    static class E {
        private final int instance = instances++;


        @JsonProperty
        @ToString.Exclude
        ASimple a;

        @JsonProperty
        @ToString.Exclude
        AWithContext ac;

        public String toString () {
            return "E:" + instance;
        }
    }

    @JsonDeserialize(using = FDeserializer.class)
    static class F {
        String value;

        boolean unmarshalled = false;

        public void afterUnmarshal(Object parent) {
            unmarshalled = true;

        }
    }

    static class FDeserializer extends JsonDeserializer<F> {

        @Override
        public F deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            F f = new F();
            Object parent = AfterUnmarshalDeserializer.getParent(p);
            ObjectNode on = p.readValueAsTree();
            f.value = on.get("falue").textValue();
            AfterUnmarshalDeserializer.invokeAfterUnmarshal(ctxt, f, parent);
            return f;
        }
    }

    @Test
    public void testB() throws JsonProcessingException {
        B b = Jackson2Mapper.getPublisherInstance().readValue("{'a': {}}", B.class);
        assertThat(b.a.unmarshalled).isTrue();
        assertThat(b.a.parent).isEqualTo(b);

        // just that that jaxb is behaving this way too.
        B xb = JAXB.unmarshal(new StringReader("<b><a /></b>"), B.class);
        assertThat(xb.a.unmarshalled).isTrue();
    }

    @Test
    public void testC() throws JsonProcessingException {
        C c = Jackson2Mapper.getPublisherInstance().readValue("{'as': [{}]}", C.class);
        assertThat(c.as.get(0).parent).isEqualTo(c);
    }

     @Test
    public void testD() throws JsonProcessingException {
        D d = Jackson2Mapper.getPublisherInstance().readValue("{'a': {}, 'moreAs': [{}]}", D.class);
         assertThat(d.a.parent).isEqualTo(d);
         assertThat(d.moreAs.get(0).parent).isEqualTo(d);
    }


    @Test
    public void testE() throws JsonProcessingException {
        E e = Jackson2Mapper.getPublisherInstance().readValue("{'a': {}, 'ac': {}}", E.class);
        assertThat(e.a.parent).isEqualTo(e);
        assertThat(e.ac.parent).isEqualTo(e);
        assertThat(e.a.converted).isTrue();

    }

    @Test
    public void testErrorneous() {
        assertThatThrownBy(() -> {
            Jackson2Mapper.getPublisherInstance().readValue("{'as': [{}]}", ErrorneousA.class); }
        ).isInstanceOf(JsonProcessingException.class);
    }


    @Test
    public void testWithCustomDeserializer() throws JsonProcessingException {
        F f = Jackson2Mapper.getPublisherInstance().readValue("{'falue': 'foobar'}", F.class);
        assertThat(f.value).isEqualTo("foobar");
        assertThat(f.unmarshalled).isTrue();
    }
}

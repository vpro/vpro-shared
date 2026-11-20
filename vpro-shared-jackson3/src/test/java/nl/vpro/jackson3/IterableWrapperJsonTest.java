package nl.vpro.jackson3;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.StringWriter;
import java.util.*;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import nl.vpro.util.IterableWrapper;

import static org.assertj.core.api.Assertions.assertThat;

class IterableWrapperJsonTest {

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class A {
        String x = "X";

        public A() {
        }

        public A(String x) {
            this.x = x;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof A a && Objects.equals(a.x, x);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(x);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @JsonSerialize(using = IterableWrapperJson.Serializer.class)
    @JsonDeserialize(using = AWrapper.Deserializer.class)
    public static class AWrapper implements IterableWrapper<A> {
        public static class Deserializer extends IterableWrapperJson.Deserializer<AWrapper, A> {
            Deserializer() {
                super(AWrapper::new, A.class);
            }
        }

        List<A> values;

        public AWrapper() {
        }

        public AWrapper(List<A> v) {
            values = v;
        }

        @Override
        public List<A> asList() {
            return values;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @JsonSerialize(using = IterableWrapperJson.Serializer.class)
    @JsonDeserialize(using = StringWrapper.Deserializer.class)
    public static class StringWrapper implements IterableWrapper<String> {
        public static class Deserializer extends IterableWrapperJson.Deserializer<StringWrapper, String> {
            Deserializer() {
                super(StringWrapper::new, String.class);
            }
        }

        List<String> values;

        public StringWrapper() {
        }

        public StringWrapper(List<String> v) {
            values = v;
        }

        @Override
        public List<String> asList() {
            return values;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static class Container {
        private StringWrapper testList;

        public Container() {
        }

        public Container(StringWrapper tl) {
            this.testList = tl;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static class AContainer {
        private AWrapper testList;

        public AContainer() {
        }

        public AContainer(AWrapper tl) {
            this.testList = tl;
        }
    }

    @Test
    void aValues() throws Exception {
        AWrapper in = new AWrapper(Arrays.asList(new A("x"), new A("y")));
        AWrapper out = roundTripAndSimilar(in, "[{\"x\":\"x\"},{\"x\":\"y\"}]");
        assertThat(out.values).isEqualTo(in.values);
    }

    @Test
    void aSingleValue() throws Exception {
        AWrapper in = new AWrapper(Collections.singletonList(new A("a")));
        AContainer out = roundTripAndSimilar(new AContainer(in), "{\"testList\":{\"x\":\"a\"}}");
        assertThat(out.testList.values).isEqualTo(in.values);
    }

    @Test
    void aEmpty() throws Exception {
        AWrapper in = new AWrapper(new ArrayList<>());
        AContainer out = roundTripAndSimilar(new AContainer(in), "{\"testList\":[]}");
        assertThat(out.testList.values).isEmpty();
    }

    @Test
    void stringValues() throws Exception {
        StringWrapper in = new StringWrapper(Arrays.asList("a", "b"));
        StringWrapper out = roundTripAndSimilar(in, "[\"a\",\"b\"]");
        assertThat(out.values).isEqualTo(in.values);
    }

    @Test
    void stringSingleValue() throws Exception {
        StringWrapper in = new StringWrapper(Collections.singletonList("a"));
        Container out = roundTripAndSimilar(new Container(in), "{\"testList\":\"a\"}");
        assertThat(out.testList.values).isEqualTo(in.values);
    }

    @SuppressWarnings("unchecked")
    static <T> T roundTripAndSimilar(T input, String expected) throws Exception {
        StringWriter writer = new StringWriter();
        Jackson3Mapper.INSTANCE.writer().writeValue(writer, input);
        String text = writer.toString();
        JSONAssert.assertEquals("\n" + text + "\nis different from expected\n" + expected, expected, text, JSONCompareMode.LENIENT);
        return (T) Jackson3Mapper.INSTANCE.readerFor(input.getClass()).readValue(text);
    }
}

package nl.vpro.jackson2;

import java.io.StringWriter;
import java.util.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 4.2
 */
public class IterableJsonTest {

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class A {
        String x = "X";

        public A() {

        }
        public A(String x) {
            this.x = x;
        }
        public boolean equals(Object other){
            return other != null && other instanceof A && Objects.equals(((A) other).x, x);

        }
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @JsonSerialize(using = IterableJson.Serializer.class)
    @JsonDeserialize(using = AIterable.Deserializer.class)
    public static class AIterable implements Iterable<A> {
        public static class Deserializer extends IterableJson.Deserializer<A> {
            Deserializer() {
                super(AIterable::new, A.class);
            }
        }

        List<A> values;

        public AIterable() {

        }

        public AIterable(List<A> v) {
            values = v;
        }

        @NonNull
        @Override
        public Iterator<A> iterator() {
            return values.iterator();

        }

    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @JsonSerialize(using = IterableJson.Serializer.class)
    @JsonDeserialize(using = StringIterable.Deserializer.class)
    public static class StringIterable implements Iterable<String> {
        public static class Deserializer extends IterableJson.Deserializer<String> {
            Deserializer() {
                super(StringIterable::new, String.class);
            }
        }

        List<String> values;

        public StringIterable() {

        }

        public StringIterable(@NonNull List<String> v) {
            values = v;
        }

        @Override
        @NonNull
        public Iterator<String> iterator() {
            return values.iterator();

        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static class Container {
        private StringIterable testList;

        public Container() {

        }
        public Container(StringIterable tl) {
            this.testList = tl;
        }
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    static class AContainer {
        private AIterable testList;

        public AContainer() {

        }

        public AContainer(AIterable tl) {
            this.testList = tl;
        }
    }


    @Test
    public void testGetAValueJson() throws Exception {
        AIterable in = new AIterable(Arrays.asList(new A("x"), new A("y")));

        AIterable out = roundTripAndSimilar(in, "[{\"x\":\"x\"},{\"x\":\"y\"}]");

        assertThat(out.values).isEqualTo(in.values);

    }

    @Test
    public void testGetAValueJsonSingleValue() throws Exception {
        AIterable in = new AIterable(Collections.singletonList(new A("a")));

        AContainer out = roundTripAndSimilar(new AContainer(in), "{\"testList\":{\"x\":\"a\"}}");

        assertThat(out.testList.values).isEqualTo(in.values);

    }


    @Test
    public void testGetValueJson() throws Exception {
        StringIterable in = new StringIterable(Arrays.asList("a", "b"));

        StringIterable out = roundTripAndSimilar(in, "[\"a\",\"b\"]");

        assertThat(out.values).isEqualTo(in.values);

    }

    @Test
    public void testGetValueJsonSingleValue() throws Exception {
        StringIterable in = new StringIterable(Collections.singletonList("a"));

        Container out = roundTripAndSimilar(new Container(in), "{\"testList\":\"a\"}");

        assertThat(out.testList.values).isEqualTo(in.values);

    }

    @SuppressWarnings("unchecked")
    static <T> T roundTripAndSimilar(T input, String expected) throws Exception {
        StringWriter writer = new StringWriter();
        Jackson2Mapper.getInstance().writeValue(writer, input);

        String text = writer.toString();

        JSONAssert.assertEquals("\n" + text + "\nis different from expected\n" + expected, expected, text, JSONCompareMode.LENIENT);

        return (T) Jackson2Mapper.getInstance().readValue(text, input.getClass());

    }


}

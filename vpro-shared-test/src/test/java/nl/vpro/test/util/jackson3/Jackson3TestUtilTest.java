package nl.vpro.test.util.jackson3;


import lombok.Getter;

import jakarta.xml.bind.annotation.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
public class Jackson3TestUtilTest {

    @SuppressWarnings("FieldMayBeFinal")
    @XmlRootElement
    @XmlType
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class A {
        private String a = "a";
        public A() {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            A a1 = (A) o;

            return a != null ? a.equals(a1.a) : a1.a == null;

        }

        @Override
        public int hashCode() {
            return a != null ? a.hashCode() : 0;
        }
    }

    @Getter
    public static class NotUnmarshable {
        private final String string;

        public NotUnmarshable(String a) {
            this.string = a;
        }
    }

    @Test
    public void roundTrip() throws Exception {
        Jackson3TestUtil.roundTrip(new A());
    }


    @Test
    public void roundTripAndSimilar() {
        Jackson3TestUtil.roundTripAndSimilar(new A(), "{'a': 'a'}");
    }

    @Test
    public void roundTripAndSimilarFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            Jackson3TestUtil.roundTripAndSimilar(new A(), "{'a': 'b'}"));
    }

    @Test
    public void roundTripAndSimilarValue() {
        Jackson3TestUtil.roundTripAndSimilarValue("a", "\"a\"");
    }

    @Test
    public void roundTripAndSimilarValueFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            Jackson3TestUtil.roundTripAndSimilarValue("a", "\"b\"")
        );
    }

    @Test
    public void testNotunmarshable() {
        Jackson3TestUtil
            .assertThatJson(new NotUnmarshable("x"))
            .withoutUnmarshalling()
            .isSimilarTo("{\n" +
            "  \"string\" : \"x\"\n" +
            "}");
    }

}

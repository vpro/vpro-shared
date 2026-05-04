package nl.vpro.test.util.jackson2;


import com.fasterxml.jackson.databind.JsonNode;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
public class Jackson2TestUtilTest {

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
        Jackson2TestUtil.roundTrip(new A());
    }


    @Test
    public void assertThatTest() {
        JsonNode actual = Jackson2TestUtil.assertThatJson(new A())
            .isSimilarTo("{'a': 'a'}")
            .actualJson();
        assertThat(actual.get("a").textValue()).isEqualTo("a");
    }



    @Test
    public void roundTripAndSimilar() {
        Jackson2TestUtil.roundTripAndSimilar(new A(), "{'a': 'a'}");
    }

    @Test
    public void roundTripAndSimilarFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            Jackson2TestUtil.roundTripAndSimilar(new A(), "{'a': 'b'}"));
    }



    @Test
    public void roundTripAndSimilarValueFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            Jackson2TestUtil.roundTripAndSimilarValue("a", "\"b\"")
        );
    }

    @Test
    public void testNotunmarshable() {
        Jackson2TestUtil
            .assertThatJson(new NotUnmarshable("x"))
            .containsKeys("string")
            .withoutUnmarshalling()
            .isSimilarTo("""
                {
                  "string" : "x"
                }""");

    }

}

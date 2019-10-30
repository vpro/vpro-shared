package nl.vpro.test.util.jackson2;


import javax.xml.bind.annotation.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
public class Jackson2TestUtilTest {

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
    @Test
    public void roundTrip() throws Exception {
        Jackson2TestUtil.roundTrip(new A());
    }


    @Test
    public void roundTripAndSimilar() throws Exception {
        Jackson2TestUtil.roundTripAndSimilar(new A(), "{'a': 'a'}");
    }

    @Test
    public void roundTripAndSimilarFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            Jackson2TestUtil.roundTripAndSimilar(new A(), "{'a': 'b'}"));
    }

    @Test
    public void roundTripAndSimilarValue() throws Exception {
        Jackson2TestUtil.roundTripAndSimilarValue("a", "\"a\"");
    }

    @Test
    public void roundTripAndSimilarValueFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            Jackson2TestUtil.roundTripAndSimilarValue("a", "\"b\"")
        );
    }

}

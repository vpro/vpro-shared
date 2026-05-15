package nl.vpro.test.util.jackson2;


import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.xml.bind.annotation.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
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
        String b = null;
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
        JsonNode actual = assertThatJson(new A())
            .isSimilarTo("{'a': 'a'}")
            .actualJson((a) -> {
                assertThat(a.get("a").textValue()).isEqualTo("a");
                assertFail(() ->{
                    assertThat(a.get("a").textValue()).isEqualTo("b");
                });
            })
            .actualJson();
        assertThat(actual.get("a").textValue()).isEqualTo("a");
    }

    @Test
    public void assertThatWithIgnore() {
        A a = new A();
        a.b = RandomStringUtils.insecure().nextAlphanumeric(10);
        assertThatJson(a)
            .ignore("/b")
            .isSimilarTo("{'a': 'a'}")
            .actualJson((j) -> {
                assertThat(j.get("b").textValue()).isNotEmpty();
            })
        ;
    }


    @Test
    public void assertThatWithIgnoreArrayElement() {

        assertThatJson("""
             {
                   "user" : null,
                   "filename" : null,
                   "errors" : [ {
                     "messages" : [ "maag niet null zijn" ]
                   } ],
                   "total" : 1
                 }
             """)
            .ignore("/errors/0/messages/0")
            .isSimilarTo("""
                 {
                                   "user" : null,
                                   "filename" : null,
                                   "errors" : [ {
                                     "messages" : [ "mag niet null zijn" ]
                                   } ],
                                   "total" : 1
                                 }
                             ""\"
                """)
        ;
    }

    @Test
    public void assertThatWithContains() {
        A a = new A();
        a.b = null;
        assertFail(() -> {
            assertThatJson(a)
                .containsKeys("b");
        });
        assertFail(() -> {
            assertThatJson(a)
                .doesNotContainKeys("a");
        });

        assertThatJson(a)
            .containsKeys("a")
            .doesNotContainKeys("b");

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
        assertThatJson(new NotUnmarshable("x"))
            .containsKeys("string")
            .withoutUnmarshalling()
            .isSimilarTo("""
                {
                  "string" : "x"
                }""");

    }


    static void assertFail(Runnable runnable) {
        AtomicBoolean failed = new AtomicBoolean(false);
        try {
            runnable.run();
        } catch (AssertionError e) {
            failed.set(true);
        }
        assertThat(failed.get()).isTrue();
    }
}

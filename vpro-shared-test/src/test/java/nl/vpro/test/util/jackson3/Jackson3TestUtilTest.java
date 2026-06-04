package nl.vpro.test.util.jackson3;

import lombok.Getter;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.xml.bind.annotation.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.Jackson3JsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.Jackson3MappingProvider;

import nl.vpro.jackson3.Jackson3Mapper;

import static nl.vpro.test.util.jackson3.Jackson3TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;


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
    public void roundTrip() {
        Jackson3TestUtil.roundTrip(new A());
    }


    @Test
    public void assertThatTest() {
        JsonNode actual = assertThatJson(MAPPER, new A())
            .isSimilarTo("{'a': 'a'}")
            .actualJson((a) -> {
                assertThat(a.get("a").stringValue()).isEqualTo("a");
                assertFail(() -> assertThat(a.get("a").stringValue()).isEqualTo("b"));
            })
            .actualJson();
        assertThat(actual.get("a").stringValue()).isEqualTo("a");
    }


    @Test
    public void assertThatWithRemove() {
        Jackson3TestUtilTest.A a = new Jackson3TestUtilTest.A();
        a.b = RandomStringUtils.insecure().nextAlphanumeric(10);
        assertThatJson(a)
            .remove("/b")
            .isSimilarTo("{'a': 'a'}")
            .actualJson((j) -> {
                AssertionsForClassTypes.assertThat(j.get("b").stringValue()).isNotEmpty();
            })
        ;
    }

    @Test
    public void removeJsonPointerArray() {
        JsonNode node = Jackson3Mapper.getLenientInstance().reader().readTree("""
              {
                   "user" : null,
                   "filename" : null,
                   "errors" : [ {
                     "messages" : [ "mag niet null zijn" ]
                   } ],
                   "total" : 1
                 }
            """);
        Jackson3TestUtil.remove(node, JsonPointer.compile("/errors/0/messages/0"));
        AssertionsForClassTypes.assertThat(node.get("errors").get(0).get("messages").size()).isEqualTo(0);
    }


    @Test
    public void assertThatWithIgnoreArrayElement() {

        assertThatJson("""
             {
                   "user" : null,
                   "filename" : null,
                   "errors" : [ {
                     "messages" : [ "mag niet null zijn" ]
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
                     "messages" : [ "IGNORED" ]
                   } ],
                   "total" : 1
                 }
             """)
        ;
    }

    @Test
    public void assertThatWithRemoveArrayElement() {

        assertThatJson("""
             {
                   "user" : null,
                   "filename" : null,
                   "errors" : [ {
                     "messages" : [ "mag niet null zijn" ]
                   } ],
                   "total" : 1
                 }
             """)
            .remove("/errors/0/messages/0")
            .isSimilarTo("""
                 {
                   "user" : null,
                   "filename" : null,
                   "errors" : [ {
                     "messages" : [  ]
                   } ],
                   "total" : 1
                 }
             """)
        ;
    }

    @Test
    public void assertThatWithContains() {
        A a = new A();
        a.b = null;
        assertFail(() ->
            assertThatJson(a).containsKeys("b")
        );
        assertFail(() ->
            assertThatJson(a).doesNotContainKeys("a")
        );
        assertThatJson(a)
            .containsKeys("a")
            .doesNotContainKeys("b");

    }





    @Test
    public void testRoundTripAndSimilar() {
        roundTripAndSimilar(new A(), "{'a': 'a'}");
    }

    @Test
    public void roundTripAndSimilarFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            roundTripAndSimilar(new A(), "{'a': 'b'}"));
    }

    @Test
    public void roundTripAndSimilarValueFail() {
        Assertions.assertThrows(AssertionError.class, () ->
            roundTripAndSimilarValue("a", "\"b\"")
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

    @Test
    public void prettifyNull() {
        assertThat(prettify(null)).isNull();
    }
    @Test
    public void removePointers() {
        assertThatJson(
            """
            {
            "a": "a",
            "b": "b",
            "c": [1, 2, 3]
            }""".getBytes(StandardCharsets.UTF_8))
            .remove("/b", "/c/1")
            .isSimilarTo("""
            {
              "a": "a",
              "c": [1, 3]
            }
            """);

    }

    @Test
    public void removeByJsonPath() {
        Configuration config = Configuration.builder()
            .jsonProvider(new Jackson3JsonNodeJsonProvider())
            .mappingProvider(new Jackson3MappingProvider())
            .build();
        ParseContext context = JsonPath.using(config);
        assertThatJson(
            """
            {
            "a": "a",
            "b": "b",
            "c": [1, 2, 3]
            }""".getBytes(StandardCharsets.UTF_8))
            .beforeComparison(j ->
                context.parse(j)
                    .delete("$.b")
                    .delete("$.c[1]")
                    .json()
            )
            .isSimilarTo("""
            {
              "a": "a",
              "c": [1, 3]
            }
            """);

    }
    @Test
    public void operate() {
        assertThatJson(
            """
            {
            "a": "a",
            "b": "b",
            "c": [1, 2, 3]
            }""".getBytes(StandardCharsets.UTF_8))
            .beforeComparisonOperate(j -> {
                j =  MAPPER.readTree("""
                    {
                      "foo": 1
                    }
                    """
                    );
                return j;
            })
            .isSimilarTo("""
                {
                     "foo" : 1
                   }
            """);

    }


    @Test
    public void ignorePointers() {
        assertThatJson(
            """
            {
            "a": "a",
            "b": "b",
            "c": [1, 2, 3]
            }""".getBytes(StandardCharsets.UTF_8))
            .ignore("/b", "/c/1")
            .isSimilarTo("""
                {
                   "a" : "a",
                   "b" : "IGNORED",
                   "c" : [ 1, "IGNORED", 3 ]
                 }
            """);

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

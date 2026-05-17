package nl.vpro.test.util.jackson2;


import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.xml.bind.annotation.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import nl.vpro.jackson2.Jackson2Mapper;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
@Log4j2
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
    public void assertThatWithRemove() {
        A a = new A();
        a.b = RandomStringUtils.insecure().nextAlphanumeric(10);
        assertThatJson(a)
            .remove("/b")
            .isSimilarTo("{'a': 'a'}")
            .actualJson((j) -> {
                assertThat(j.get("b").textValue()).isNotEmpty();
            })
        ;
    }

    @Test
    public void removeJsonPointerArray() throws JsonProcessingException {
        JsonNode node = Jackson2Mapper.getLenientInstance().readTree("""
              {
                   "user" : null,
                   "filename" : null,
                   "errors" : [ {
                     "messages" : [ "mag niet null zijn" ]
                   } ],
                   "total" : 1
                 }
            """);
        Jackson2TestUtil.remove(node, JsonPointer.compile("/errors/0/messages/0"));
        assertThat(node.get("errors").get(0).get("messages").size()).isEqualTo(0);
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

    @Test
    public void removePointers() {
        Jackson2TestUtil.assertThatJson(
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
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
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
    public void ignorePointers() {
        Jackson2TestUtil.assertThatJson(
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

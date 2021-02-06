package nl.vpro.elasticsearchclient;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;

/**
 * @author Michiel Meeuwissen
 */
class QueryBuilderTest {

    @Test
    void asc() {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        QueryBuilder.asc(request, "title");
        assertThatJson(request).isSimilarTo("{\n" +
            "  \"sort\" : [ {\n" +
            "    \"title\" : \"asc\"\n" +
            "  } ]\n" +
            "}");

    }

    @Test
    void desc() {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        QueryBuilder.desc(request, "title");
        assertThatJson(request).isSimilarTo("{\n" +
            "  \"sort\" : [ {\n" +
            "    \"title\" : \"desc\"\n" +
            "  } ]\n" +
            "}");
    }

    @Test
    void docOrder() {
        ObjectNode request = Jackson2Mapper.getInstance().createObjectNode();
        QueryBuilder.docOrder(request);
        assertThatJson(request).isSimilarTo("{\n" +
            "  \"sort\" : [ \"_doc\" ]\n" +
            "}");
    }

    @Test
    void must() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode must = QueryBuilder.must(q);
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"must\" : [ { } ]\n" +
            "  }\n" +
            "}");
    }

    @Test
    void filter() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode filter = QueryBuilder.filter(q);
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"filter\" : [ { } ]\n" +
            "  }\n" +
            "}");
    }

    @Test
    void mustTerm() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode must =  QueryBuilder.mustTerm(q, "field", "foobar");
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"must\" : [ {\n" +
            "      \"term\" : {\n" +
            "        \"field\" : \"foobar\"\n" +
            "      }\n" +
            "    } ]\n" +
            "  }\n" +
            "}");
    }

    @Test
    void mustWildcard() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode must =  QueryBuilder.mustWildcard(q, "field", "foobar");
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"must\" : [ {\n" +
            "      \"wildcard\" : {\n" +
            "        \"field\" : \"foobar\"\n" +
            "      }\n" +
            "    } ]\n" +
            "  }\n" +
            "}");
    }

    @Test
    void filterTerm() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode filterTerm =  QueryBuilder.filterTerm(q, "field", "foobar");
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"filter\" : [ {\n" +
            "      \"term\" : {\n" +
            "        \"field\" : \"foobar\"\n" +
            "      }\n" +
            "    } ]\n" +
            "  }\n" +
            "}");
    }

    @Test
    void should() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode should =  QueryBuilder.should(q);
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"should\" : [ { } ]\n" +
            "  }\n" +
            "}");
    }

    @Test
    void shouldTerm() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode should =  QueryBuilder.shouldTerm(q, "title", "foobar");
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"bool\" : {\n" +
            "    \"should\" : [ {\n" +
            "      \"term\" : {\n" +
            "        \"title\" : \"foobar\"\n" +
            "      }\n" +
            "    } ]\n" +
            "  }\n" +
            "}");
    }

    @Test
    void longRange() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode range = QueryBuilder.range(q, "long", -100L, 200L);
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"range\" : {\n" +
            "    \"long\" : {\n" +
            "      \"gte\" : -100,\n" +
            "      \"lt\" : 200\n" +
            "    }\n" +
            "  }\n" +
            "}");
    }
    @Test
    void longRangeStop() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode range =  QueryBuilder.range(q, "long", null, 200L);
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"range\" : {\n" +
            "    \"long\" : {\n" +
            "      \"lt\" : 200\n" +
            "    }\n" +
            "  }\n" +
            "}");
    }

    @Test
    void longRangeStart() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode range =  QueryBuilder.range(q, "long", 100L, null);
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"range\" : {\n" +
            "    \"long\" : {\n" +
            "      \"gte\" : 100\n" +
            "    }\n" +
            "  }\n" +
            "}");
    }


    @Test
    void instantRange() {
        ObjectNode q = Jackson2Mapper.getInstance().createObjectNode();
        ObjectNode range =  QueryBuilder.range(q, "long", Instant.ofEpochMilli(1612639098121L), Instant.ofEpochMilli(1612639198121L));
        assertThatJson(q).isSimilarTo("{\n" +
            "  \"range\" : {\n" +
            "    \"long\" : {\n" +
            "      \"gte\" : 1612639098121,\n" +
            "      \"lt\" : 1612639198121\n" +
            "    }\n" +
            "  }\n" +
            "}");
    }


}

package nl.vpro.elasticsearchclient;

import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import nl.vpro.jackson3.Jackson3Mapper;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;

/**
 * @author Michiel Meeuwissen
 */
class QueryBuilderTest {

    @Test
    void asc() {
        ObjectNode request = Jackson3Mapper.getInstance().writer().createObjectNode();
        QueryBuilder.asc(request, "title");
        assertThatJson(request).isSimilarTo("""
            {
              "sort" : [ {
                "title" : "asc"
              } ]
            }""");

    }

    @Test
    void desc() {
        ObjectNode request = Jackson3Mapper.getInstance().writer().createObjectNode();
        QueryBuilder.desc(request, "title");
        assertThatJson(request).isSimilarTo("""
            {
              "sort" : [ {
                "title" : "desc"
              } ]
            }""");
    }

    @Test
    void docOrder() {
        ObjectNode request = Jackson3Mapper.getInstance().writer().createObjectNode();
        QueryBuilder.docOrder(request);
        assertThatJson(request).isSimilarTo("""
            {
              "sort" : [ "_doc" ]
            }""");
    }

    @Test
    void must() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode must = QueryBuilder.must(q);
        assertThatJson(q).isSimilarTo("""
            {
              "bool" : {
                "must" : [ { } ]
              }
            }""");
    }

    @Test
    void filter() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode filter = QueryBuilder.filter(q);
        assertThatJson(q).isSimilarTo("""
            {
              "bool" : {
                "filter" : [ { } ]
              }
            }""");
    }

    @Test
    void mustTerm() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode must =  QueryBuilder.mustTerm(q, "field", "foobar");
        assertThatJson(q).isSimilarTo("""
            {
              "bool" : {
                "must" : [ {
                  "term" : {
                    "field" : "foobar"
                  }
                } ]
              }
            }""");
    }

    @Test
    void mustWildcard() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode must =  QueryBuilder.mustWildcard(q, "field", "foobar");
        assertThatJson(q).isSimilarTo("""
            {
              "bool" : {
                "must" : [ {
                  "wildcard" : {
                    "field" : "foobar"
                  }
                } ]
              }
            }""");
    }

    @Test
    void filterTerm() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode filterTerm =  QueryBuilder.filterTerm(q, "field", "foobar");
        assertThatJson(q).isSimilarTo("""
            {
              "bool" : {
                "filter" : [ {
                  "term" : {
                    "field" : "foobar"
                  }
                } ]
              }
            }""");
    }

    @Test
    void should() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode should =  QueryBuilder.should(q);
        assertThatJson(q).isSimilarTo("""
            {
              "bool" : {
                "should" : [ { } ]
              }
            }""");
    }

    @Test
    void shouldTerm() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode should =  QueryBuilder.shouldTerm(q, "title", "foobar");
        assertThatJson(q).isSimilarTo("""
            {
              "bool" : {
                "should" : [ {
                  "term" : {
                    "title" : "foobar"
                  }
                } ]
              }
            }""");
    }

    @Test
    void longRange() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode range = QueryBuilder.range(q, "long", -100L, 200L);
        assertThatJson(q).isSimilarTo("""
            {
              "range" : {
                "long" : {
                  "gte" : -100,
                  "lt" : 200
                }
              }
            }""");
    }
    @Test
    void longRangeStop() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode range =  QueryBuilder.range(q, "long", null, 200L);
        assertThatJson(q).isSimilarTo("""
            {
              "range" : {
                "long" : {
                  "lt" : 200
                }
              }
            }""");
    }

    @Test
    void longRangeStart() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode range =  QueryBuilder.range(q, "long", 100L, null);
        assertThatJson(q).isSimilarTo("""
            {
              "range" : {
                "long" : {
                  "gte" : 100
                }
              }
            }""");
    }


    @Test
    void instantRange() {
        ObjectNode q = Jackson3Mapper.getInstance().writer().createObjectNode();
        ObjectNode range =  QueryBuilder.range(q, "long", Instant.ofEpochMilli(1612639098121L), Instant.ofEpochMilli(1612639198121L));
        assertThatJson(q).isSimilarTo("""
            {
              "range" : {
                "long" : {
                  "gte" : 1612639098121,
                  "lt" : 1612639198121
                }
              }
            }""");
    }


}

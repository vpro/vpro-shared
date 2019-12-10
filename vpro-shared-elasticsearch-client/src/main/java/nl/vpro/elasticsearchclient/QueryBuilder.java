package nl.vpro.elasticsearchclient;

import java.time.Instant;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
public class QueryBuilder {
    protected static void sort(ObjectNode request, String field, String dir){
        ArrayNode sort = request.withArray("sort");
        ObjectNode sortNode  = sort.addObject();
        sortNode.put(field, dir);
    }

    public static void asc(ObjectNode request, String field) {
        sort(request, field, "asc");
    }

    public static void desc(ObjectNode request, String field) {
        sort(request, field, "desc");
    }

    public static void docOrder(ObjectNode request) {
        ArrayNode sort = request.withArray("sort");
        sort.add("_doc");
    }

    public static ObjectNode must(ObjectNode query) {
        ObjectNode bool = query.with("bool");
        ArrayNode must = bool.withArray("must");
        ObjectNode clause = must.addObject();
        return clause;
    }
    public static ObjectNode filter(ObjectNode query) {
        ObjectNode bool = query.with("bool");
        ArrayNode must = bool.withArray("filter");
        ObjectNode clause = must.addObject();
        return clause;
    }

    public static ObjectNode mustTerm(ObjectNode query, String field, String value) {
        ObjectNode clause = must(query);
        ObjectNode term = clause.with("term");
        term.put(field, value);
        return clause;
    }
    public static ObjectNode filterTerm(ObjectNode query, String field, String value) {
        ObjectNode clause = filter(query);
        ObjectNode term = clause.with("term");
        term.put(field, value);
        return clause;
    }

    public static ObjectNode should(ObjectNode query) {
        ObjectNode bool = query.with("bool");
        ArrayNode must = bool.withArray("should");
        ObjectNode clause = must.addObject();
        return clause;
    }

    public static ObjectNode shouldTerm(ObjectNode query, String field, String value) {
        ObjectNode clause = should(query);
        ObjectNode term = clause.with("term");
        term.put(field, value);
        return clause;
    }

    public static ObjectNode range(ObjectNode query, String field, Instant start, Instant stop) {
        ObjectNode range = query.with("range");
        ObjectNode fieldObject = range.with(field);
        if (start != null) {
            fieldObject.put("gte", start.toEpochMilli());
        }
        if (stop != null) {
            fieldObject.put("lt", stop.toEpochMilli());
        }
        return range;
    }

}

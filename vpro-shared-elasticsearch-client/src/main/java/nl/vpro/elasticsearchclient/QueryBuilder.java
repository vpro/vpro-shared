package nl.vpro.elasticsearchclient;

import java.time.Instant;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static nl.vpro.elasticsearch.Constants.*;
import static nl.vpro.elasticsearch.Constants.Query.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
public class QueryBuilder {
    protected static void sort(ObjectNode request, String field, String dir){
        ArrayNode sort = request.withArray(SORT);
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
        ArrayNode sort = request.withArray(SORT);
        sort.add("_doc");
    }

    public static ObjectNode must(ObjectNode query) {
        ObjectNode bool = query.with(BOOL);
        ArrayNode must = bool.withArray(MUST);
        ObjectNode clause = must.addObject();
        return clause;
    }
    public static ObjectNode filter(ObjectNode query) {
        ObjectNode bool = query.with(BOOL);
        ArrayNode must = bool.withArray(FILTER);
        ObjectNode clause = must.addObject();
        return clause;
    }

    public static ObjectNode mustTerm(ObjectNode query, String field, String value) {
        ObjectNode clause = must(query);
        ObjectNode term = clause.with(TERM);
        term.put(field, value);
        return clause;
    }
    public static ObjectNode filterTerm(ObjectNode query, String field, String value) {
        ObjectNode clause = filter(query);
        ObjectNode term = clause.with(TERM);
        term.put(field, value);
        return clause;
    }

    public static ObjectNode should(ObjectNode query) {
        ObjectNode bool = query.with(BOOL);
        ArrayNode must = bool.withArray(SHOULD);
        ObjectNode clause = must.addObject();
        return clause;
    }

    public static ObjectNode shouldTerm(ObjectNode query, String field, String value) {
        ObjectNode clause = should(query);
        ObjectNode term = clause.with(TERM);
        term.put(field, value);
        return clause;
    }

    public static ObjectNode range(ObjectNode query, String field, Instant start, Instant stop) {
        return range(query, field, start == null ? null : start.toEpochMilli(), stop == null ? null : stop.toEpochMilli());
    }

    public static ObjectNode range(ObjectNode query, String field, Long start, Long stop) {
        ObjectNode range = query.with(RANGE);
        ObjectNode fieldObject = range.with(field);
        if (start != null) {
            fieldObject.put("gte", start);
        }
        if (stop != null) {
            fieldObject.put("lt", stop);
        }
        return range;
    }

}

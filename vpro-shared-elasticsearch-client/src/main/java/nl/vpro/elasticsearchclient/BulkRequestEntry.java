package nl.vpro.elasticsearchclient;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.Constants;

/**
 * @author Michiel Meeuwissen
 * @since 2.10
 */
@Getter
@Slf4j
public class BulkRequestEntry {

    final ObjectNode action;
    final ObjectNode source;
    final Map<String, String> mdc;

    @MonotonicNonNull
    String id;

    public BulkRequestEntry(ObjectNode action, ObjectNode source, Map<String, String> mdc) {
        this.action = action;
        this.source = source;
        this.mdc = mdc;
    }

    public String getId() {
        if (id == null) {
            id = idFromActionNode(action);
        }
        return id;
    }

    public static String idFromActionNode(ObjectNode action) {
        StringBuilder builder = new StringBuilder();
        JsonNode idNode;
        if (action.has("index")) {
            builder.append("index");
            idNode = action.with("index");
        } else if (action.has("delete")) {
            builder.append("delete");
            idNode = action.with("delete");
        } else {
            throw new IllegalArgumentException("Unrecognized action node " + action);
        }
        builder.append('\t');
        builder.append(idNode.get(Constants.Fields.INDEX).textValue());
        builder.append('\t');
        builder.append(idNode.get(Constants.Fields.ID).textValue());
        return builder.toString();
    }

    @Override
    public int hashCode(){
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (! (object instanceof BulkRequestEntry)) {
            return false;
        }
        return getId().equals(((BulkRequestEntry) object).getId());
    }

    @Override
    public String toString() {
        return getId();
    }

}

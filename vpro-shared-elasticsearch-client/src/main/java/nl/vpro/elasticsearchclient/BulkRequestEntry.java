package nl.vpro.elasticsearchclient;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.Constants;

import static nl.vpro.elasticsearch.Constants.DELETE;
import static nl.vpro.elasticsearch.Constants.INDEX;

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
    final String id;

    public BulkRequestEntry(ObjectNode action, ObjectNode source, UnaryOperator<String> unalias, Map<String, String> mdc) {
        this.action = action;
        this.source = source;
        this.mdc = mdc;
        this.id = idFromActionNode(action, unalias);
    }

    public static String idFromActionNode(ObjectNode action) {
        return idFromActionNode(action, s -> s);
    }


    public static String idFromActionNode(ObjectNode action, UnaryOperator<String> unalias) {
        StringBuilder builder = new StringBuilder();
        JsonNode idNode;
        if (action.has(INDEX)) {
            builder.append(INDEX);
            idNode = action.with(INDEX);
        } else if (action.has(DELETE)) {
            builder.append(DELETE);
            idNode = action.with(DELETE);
        } else {
            throw new IllegalArgumentException("Unrecognized action node " + action);
        }
        builder.append('\t');
        builder.append(unalias.apply(idNode.get(Constants.Fields.INDEX).textValue()));
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

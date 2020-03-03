package nl.vpro.elasticsearchclient;

import lombok.Getter;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Michiel Meeuwissen
 * @since 2.10
 */
@Getter
public class BulkRequestEntry {

    final ObjectNode action;
    final ObjectNode source;
    final Map<String, String> mdc;

    public BulkRequestEntry(ObjectNode action, ObjectNode source, Map<String, String> mdc) {
        this.action = action;
        this.source = source;
        this.mdc = mdc;
    }
}

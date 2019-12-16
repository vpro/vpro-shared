package nl.vpro.elasticsearchclient;

import lombok.Getter;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Michiel Meeuwissen
 * @since 2.10
 */
@Getter
public class BulkRequestEntry {

    final ObjectNode action;
    final ObjectNode source;

    public BulkRequestEntry(ObjectNode action, ObjectNode source) {
        this.action = action;
        this.source = source;
    }
}

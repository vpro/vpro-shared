package nl.vpro.elasticsearchclient;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearch.Constants;

import static nl.vpro.elasticsearch.Constants.*;

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

    @Getter
    @Setter
    Consumer<ObjectNode> sourceConsumer;

    private boolean used = false;

    public BulkRequestEntry(
        ObjectNode action,
        ObjectNode source,
        @NonNull UnaryOperator<String> unalias,
        Map<String, String> mdc, Consumer<ObjectNode> consumer) {
        this(action, source,  unalias, null,  mdc, consumer);
    }

      public BulkRequestEntry(
        ObjectNode action,
        ObjectNode source,
        @NonNull UnaryOperator<String> unalias,
        Map<String, String> mdc) {
        this(action, source,  unalias,  mdc, null);
    }

    protected BulkRequestEntry(
        BulkRequestEntry bulkRequestEntry) {
        this(bulkRequestEntry.getAction(), bulkRequestEntry.getSource(), null, bulkRequestEntry.getId(), bulkRequestEntry.getMdc(), bulkRequestEntry.getSourceConsumer());
        this.used = false;
    }

    @lombok.Builder
    private BulkRequestEntry(
        ObjectNode action,
        ObjectNode source,
        @Nullable UnaryOperator<String> unalias,
        @Nullable String id,
        Map<String, String> mdc,
        Consumer<ObjectNode> sourceConsumer) {
        this.action = action;
        this.source = source;
        this.mdc = mdc;
        this.id = unalias == null ? id : idFromActionNode(action, unalias);
        if (this.id == null) {
            throw new IllegalArgumentException("No id");
        }
        this.sourceConsumer = sourceConsumer;
    }

    /**
     * As this entry is about to be used, i.e. submitted to the ES-server. this method should be called.
     * <p>
     * It marks {@link #isUsed()} to be true, and executes (it necessary) the {@link #getSourceConsumer()} on {@link #getSource()}
     */
    public void use() {
        if (! used) {
            if (sourceConsumer != null) {
                if (source == null) {
                    log.debug("No source for action {} for id {}", action, id);
                } else {
                    sourceConsumer.accept(source);
                }
            }
            used = true;
        } else {
            log.debug("Used already");
        }
    }

    public static String idFromActionNode(ObjectNode action) {
        return idFromActionNode(action, s -> s);
    }

    public static String idFromActionNode(@NonNull ObjectNode action, @NonNull UnaryOperator<String> unalias) {
        StringBuilder builder = new StringBuilder();
        JsonNode idNode;
        if (action.has(INDEX)) {
            builder.append(INDEX);
            idNode = action.withObject(P_INDEX);
        } else if (action.has(DELETE)) {
            builder.append(DELETE);
            idNode = action.withObject(P_DELETE);
        } else if (action.has(UPDATE)) {
            builder.append(UPDATE);
            idNode = action.withObject(P_UPDATE);
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

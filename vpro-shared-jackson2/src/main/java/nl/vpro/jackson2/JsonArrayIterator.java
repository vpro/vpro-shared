package nl.vpro.jackson2;

import lombok.Builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;

import nl.vpro.util.CloseableIterator;
import nl.vpro.util.CountedIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class JsonArrayIterator<T> extends UnmodifiableIterator<T> implements CloseableIterator<T>, PeekingIterator<T>, CountedIterator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonArrayIterator.class);

    private final JsonParser jp;

    private T next = null;

    private boolean needsFindNext = true;

    private Boolean hasNext;

    private final BiFunction<JsonParser, TreeNode, ? extends T> valueCreator;

    private Runnable callback;

    private boolean callBackHasRun = false;

    private final Long size;

    private final Long totalSize;

    private int foundNulls = 0;

    private Logger log = LOG;

    private long count = 0;

    public JsonArrayIterator(InputStream inputStream, Class<T> clazz) throws IOException {
        this(inputStream, clazz, null);

    }
    public JsonArrayIterator(InputStream inputStream, final Class<T> clazz, Runnable callback) throws IOException {
        this(inputStream, null, clazz, callback, null, null, null);
    }

    public JsonArrayIterator(InputStream inputStream, final BiFunction<JsonParser, TreeNode, T> valueCreator) throws IOException {
        this(inputStream, valueCreator, null, null, null, null, null);
    }


    @Builder
    private JsonArrayIterator(
        InputStream inputStream,
        final BiFunction<JsonParser, TreeNode,  T> valueCreator,
        final Class<T> valueClass,
        Runnable callback,
        String sizeField,
        String totalSizeField,
        ObjectMapper objectMapper
        ) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("No inputStream given");
        }
        this.jp = (objectMapper == null ? Jackson2Mapper.getLenientInstance() : objectMapper).getFactory().createParser(inputStream);
        this.valueCreator = valueCreator == null ? valueCreator(valueClass) : valueCreator;
        if (valueCreator != null && valueClass != null) {
            throw new IllegalArgumentException();
        }
        Long tmpSize = null;
        Long tmpTotalSize = null;
        String fieldName = null;
        if (sizeField == null) {
            sizeField = "size";
        }
        if (totalSizeField == null) {
            totalSizeField = "totalSize";
        }
        while(true) {
            JsonToken token = jp.nextToken();
            if (token == null) {
                break;
            }
            if (token == JsonToken.FIELD_NAME) {
                fieldName = jp.getCurrentName();
            }
            if (token == JsonToken.VALUE_NUMBER_INT && sizeField.equals(fieldName)) {
                tmpSize = jp.getLongValue();
            }
            if (token == JsonToken.VALUE_NUMBER_INT && totalSizeField.equals(fieldName)) {
                tmpTotalSize = jp.getLongValue();
            }
            if (token == JsonToken.START_ARRAY) break;
        }
        this.size = tmpSize;
        this.totalSize = tmpTotalSize;
        jp.nextToken();
        this.callback = callback;
    }

    private static <T> BiFunction<JsonParser, TreeNode, T> valueCreator(Class<T> clazz) {
        return (jp, tree) -> {
            try {
                return jp.getCodec().treeToValue(tree, clazz);
            } catch (JsonProcessingException e) {
                throw new ValueReadException(e);
            }
        };

    }

    public void setLogger(Logger log) {
        this.log = log;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    public Runnable getCallback() {
        return callback;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;
    }

    @Override
    public T peek() {
        findNext();
        return next;
    }

    @Override
    public T next() {
        findNext();
        if (! hasNext) {
            throw new NoSuchElementException();
        }
        T result = next;
        next = null;
        needsFindNext = true;
        hasNext = null;
        count++;
        return result;
    }


    @Override
    public Long getCount() {
        return count;
    }

    protected void findNext() {
        if(needsFindNext) {
            while(true) {
                try {
                    TreeNode tree = jp.readValueAsTree();
                    if (tree instanceof NullNode) {
                        foundNulls++;
                        continue;
                    }
                    try {
                        if (tree == null) {
                            callback();
                            hasNext = false;
                        } else {
                            if (foundNulls > 0) {
                                log.warn("Found {} nulls. Will be skipped", foundNulls);
                                count += foundNulls;
                            }

                            next = valueCreator.apply(jp, tree);
                            hasNext = true;
                        }
                        foundNulls = 0;
                        break;
                    } catch (ValueReadException jme) {
                        count++;
                        log.warn(jme.getClass() + " " + jme.getMessage() + " for\n" + tree + "\nWill be skipped");
                    }
                } catch (IOException e) {
                    callbackBeforeThrow(new RuntimeException(e));
                } catch (RuntimeException rte) {
                    callbackBeforeThrow(rte);
                }
            }
            needsFindNext = false;
        }
    }


    private void callbackBeforeThrow(RuntimeException e) {
        callback();
        next = null;
        needsFindNext = false;
        hasNext = false;
        throw e;
    }

    @Override
    public void close() throws IOException {
        callback();
        this.jp.close();

    }

    @Override
    public void finalize() {
        if (! callBackHasRun && callback != null) {
            log.warn("Callback not run in finalize. Did you not close the iterator?");
            callback.run();

        }
    }

    protected void callback() {
        if (! callBackHasRun) {
            if (callback != null) {
                callback.run();
            }
            callBackHasRun = true;
        }
    }

    public void write(OutputStream out, final Function<T, Void> logging) throws IOException {
        JsonGenerator jg = Jackson2Mapper.INSTANCE.getFactory().createGenerator(out);
        jg.writeStartObject();
        jg.writeArrayFieldStart("array");
        while (hasNext()) {
            T change = null;
            try {
                change = next();
                if (change != null) {
                    jg.writeObject(change);
                } else {
                    jg.writeNull();
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
                jg.writeObject(e.getMessage());
            }
            if (logging != null) {
                logging.apply(change);
            }

        }
        jg.writeEndArray();
        jg.writeEndObject();
        jg.flush();
    }

    @Override
    public Optional<Long> getSize() {
        return Optional.ofNullable(size);
    }

    @Override
    public Optional<Long> getTotalSize() {
        return Optional.ofNullable(totalSize);
    }



    public static class ValueReadException extends RuntimeException {

        public ValueReadException(JsonProcessingException e) {
            super(e);
        }
    }
}

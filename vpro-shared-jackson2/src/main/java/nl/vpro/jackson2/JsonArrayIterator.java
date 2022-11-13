package nl.vpro.jackson2;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

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
@Slf4j
public class JsonArrayIterator<T> extends UnmodifiableIterator<T>
    implements CloseableIterator<T>, PeekingIterator<T>, CountedIterator<T> {

    private final JsonParser jp;

    private T next = null;

    private boolean needsFindNext = true;

    private Boolean hasNext;

    private final BiFunction<JsonParser, TreeNode, ? extends T> valueCreator;

    @Getter
    @Setter
    private Runnable callback;

    private boolean callBackHasRun = false;

    private final Long size;

    private final Long totalSize;

    private int foundNulls = 0;

    @Setter
    private Logger logger = log;

    private long count = 0;

    private boolean skipNulls = true;

    public JsonArrayIterator(InputStream inputStream, Class<T> clazz) throws IOException {
        this(inputStream, clazz, null);

    }
    public JsonArrayIterator(InputStream inputStream, final Class<T> clazz, Runnable callback) throws IOException {
        this(inputStream, null, clazz, callback, null, null, null, null, null);
    }

    public JsonArrayIterator(InputStream inputStream, final BiFunction<JsonParser, TreeNode, T> valueCreator) throws IOException {
        this(inputStream, valueCreator, null, null, null, null, null, null, null);
    }


    public static class Builder<T> {


    }

    /**
     *
     * @param inputStream     The inputstream containing the json
     * @param valueCreator    A function which converts a json {@link TreeNode} to the desired objects in the iterator.
     * @param valueClass      If valueCreator is not given, simply the class of the desired object can be given
     *                        Json unmarshalling with the given objectMapper will happen.
     * @param callback        If the iterator is ready, closed or error this callback will be called.
     * @param sizeField       The size of the iterator, i.e. the size of the array represented in the json stream
     * @param totalSizeField  Sometimes the array is part of something bigger, e.g. a page in a search result. The size
     *                        of the 'complete' result can be in the beginning of the json in this field.
     * @param objectMapper    Default the objectMapper {@link Jackson2Mapper#getLenientInstance()} will be used (in
     *                        conjuction with <code>valueClass</code>, but you may specify another one
     * @param logger          Default this is logging to nl.vpro.jackson2.JsonArrayIterator, but you may override that.
     * @param skipNulls
     * @throws IOException    If the json parser could not be created or the piece until the start of the array could
     *                        not be tokenized.
     */
     @lombok.Builder(builderClassName = "Builder", builderMethodName = "builder")
    private JsonArrayIterator(
        @NonNull  InputStream inputStream,
        @Nullable final BiFunction<JsonParser, TreeNode,  T> valueCreator,
        @Nullable final Class<T> valueClass,
        @Nullable Runnable callback,
        @Nullable String sizeField,
        @Nullable String totalSizeField,
        @Nullable ObjectMapper objectMapper,
        @Nullable Logger logger,
        @Nullable Boolean skipNulls
        ) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("No inputStream given");
        }
        this.jp = (objectMapper == null ? Jackson2Mapper.getLenientInstance() : objectMapper).getFactory().createParser(inputStream);
        this.valueCreator = valueCreator == null ? valueCreator(valueClass) : valueCreator;
        if (valueCreator != null && valueClass != null) {
            throw new IllegalArgumentException();
        }
         if (logger != null) {
             this.logger = logger;
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
        // find the start of the array, where we will start iterating.
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
        this.skipNulls = skipNulls == null || skipNulls;
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
        count += foundNulls;
        foundNulls = 0;
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

                    if (jp.getLastClearedToken() == JsonToken.END_ARRAY) {
                        tree = null;
                    } else {
                        if (tree instanceof NullNode && skipNulls) {
                            foundNulls++;
                            continue;
                        }
                    }

                    try {
                        if (tree == null) {
                            callback();
                            hasNext = false;
                        } else {
                            if (foundNulls > 0) {
                                logger.warn("Found {} nulls. Will be skipped", foundNulls);
                            }

                            next = valueCreator.apply(jp, tree);
                            hasNext = true;
                        }
                        break;
                    } catch (ValueReadException jme) {
                        foundNulls++;
                        logger.warn(jme.getClass() + " " + jme.getMessage() + " for\n" + tree + "\nWill be skipped");
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
            logger.warn("Callback not run in finalize. Did you not close the iterator?");
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


    /**
     * Write the entire stream to an output stream
     */
    public void write(OutputStream out, final Consumer<T> logging) throws IOException {
        write(this, out, logging == null ? null : (c) -> { logging.accept(c); return null;});
    }

    public void writeArray(OutputStream out, final Consumer<T> logging) throws IOException {
        writeArray(this, out, logging == null ? null : (c) -> { logging.accept(c); return null;});
    }


    /**
     * Write the entire stream to an output stream
     * @deprecated Use {@link #write(OutputStream, Consumer)}
     */
    public void write(OutputStream out, final Function<T, Void> logging) throws IOException {
        write(this, out, logging);
    }

    /**
     * Write the entire stream to an output stream
     */
    public static <T> void write(
        final CountedIterator<T> iterator,
        final OutputStream out,
        final Function<T, Void> logging) throws IOException {
        try (JsonGenerator jg = Jackson2Mapper.getInstance().getFactory().createGenerator(out)) {
            jg.writeStartObject();
            jg.writeArrayFieldStart("array");
            writeObjects(iterator, jg, logging);
            jg.writeEndArray();
            jg.writeEndObject();
            jg.flush();
        }
    }

    /**
     * Write the entire stream to an output stream
     */
    public static <T> void writeArray(
        final CountedIterator<T> iterator,
        final OutputStream out, final Function<T, Void> logging) throws IOException {
        try (JsonGenerator jg = Jackson2Mapper.getInstance().getFactory().createGenerator(out)) {
            jg.writeStartArray();
            writeObjects(iterator, jg, logging);
            jg.writeEndArray();
            jg.flush();
        }
    }


    /**
     * Write the entire stream as an array to jsonGenerator
     */
    public static <T> void writeObjects(
        final CountedIterator<T> iterator,
        final JsonGenerator jg,
        final Function<T, Void> logging) throws IOException {
        while (iterator.hasNext()) {
            T change;
            try {
                change = iterator.next();
                if (change != null) {
                    jg.writeObject(change);
                } else {
                    jg.writeNull();
                }
                if (logging != null) {
                    logging.apply(change);
                }
            } catch (Exception e) {
                Throwable cause = e.getCause();
                while (cause != null) {
                    if (cause instanceof InterruptedException) {
                        return;
                    }
                    cause = cause.getCause();
                }

                log.warn(e.getClass().getCanonicalName() + " " + e.getMessage());
                jg.writeObject(e.getMessage());
            }

        }
    }


    @Override
    @NonNull
    public Optional<Long> getSize() {
        return Optional.ofNullable(size);
    }

    @Override
    @NonNull
    public Optional<Long> getTotalSize() {
        return Optional.ofNullable(totalSize);
    }



    public static class ValueReadException extends RuntimeException {

        public ValueReadException(JsonProcessingException e) {
            super(e);
        }
    }
}

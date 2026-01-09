package nl.vpro.jackson3;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.*;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.node.NullNode;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;

import nl.vpro.util.CloseableIterator;
import nl.vpro.util.CountedIterator;

import static java.util.Objects.requireNonNull;

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

    private final Listener<T> eventListener;

    public JsonArrayIterator(InputStream inputStream, Class<T> clazz) throws IOException {
        this(inputStream, clazz, null);
    }

    public JsonArrayIterator(InputStream inputStream, final Class<T> clazz, Runnable callback) throws IOException {
        this(inputStream, null, clazz, callback, null, null, null, null, null, null);
    }

    public JsonArrayIterator(InputStream inputStream, final BiFunction<JsonParser, TreeNode, T> valueCreator) throws IOException {
        this(inputStream, valueCreator, null, null, null, null, null, null, null, null);
    }


    public static class Builder<T> implements Iterable<T> {


        @Override
        public @NonNull Iterator<T> iterator() {
            JsonArrayIterator<T> build = build();
            return build.stream()
                .onClose(build::close)
                .iterator();
        }
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
     * @param objectMapper    Default the objectMapper {@link Jackson3Mapper#LENIENT} will be used (in
     *                        conjunction with <code>valueClass</code>, but you may specify another one
     * @param logger          Default this is logging to nl.vpro.jackson2.JsonArrayIterator, but you may override that.
     * @param skipNulls       Whether to skip nulls in the array. Default true.
     * @param eventListener   A listener for events that happen during parsing and iteration of the array. See {@link Event} and extension classes.
     */
     @lombok.Builder(builderClassName = "Builder", builderMethodName = "_builder")
     private JsonArrayIterator(
         @NonNull  InputStream inputStream,
         @Nullable final BiFunction<JsonParser, TreeNode,  T> valueCreator,
         @Nullable final Class<T> valueClass,
         @Nullable Runnable callback,
         @Nullable String sizeField,
         @Nullable String totalSizeField,
         @Nullable Jackson3Mapper objectMapper,
         @Nullable Logger logger,
         @Nullable Boolean skipNulls,
         @Nullable Listener<T> eventListener
     ) {
         requireNonNull(inputStream, "No inputStream given");
         ObjectReader reader = objectMapper == null ? Jackson3Mapper.LENIENT.reader() : objectMapper.reader();
         this.jp = reader.createParser(inputStream);
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
         this.eventListener = eventListener == null? Listener.noop() : eventListener;
         // find the start of the array, where we will start iterating.
         while(true) {
             JsonToken token = jp.nextToken();
             if (token == null) {
                 break;
             }
             this.eventListener.accept(new TokenEvent(token));
             if (token == JsonToken.PROPERTY_NAME) {
                 fieldName = jp.currentName();
             }
             if (token == JsonToken.VALUE_NUMBER_INT && sizeField.equals(fieldName)) {
                 tmpSize = jp.getLongValue();
                 this.eventListener.accept(new SizeEvent(tmpSize));
             }
             if (token == JsonToken.VALUE_NUMBER_INT && totalSizeField.equals(fieldName)) {
                 tmpTotalSize = jp.getLongValue();
                 this.eventListener.accept(new TotalSizeEvent(tmpTotalSize));

             }
             if (token == JsonToken.START_ARRAY) {
                 break;
             }
         }
         this.size = tmpSize;
         this.totalSize = tmpTotalSize;
         this.eventListener.accept(new StartEvent());
         JsonToken token = jp.nextToken();
         this.needsFindNext = token != JsonToken.END_ARRAY;
         if (! needsFindNext) {
             this.hasNext = false;
         }

         this.eventListener.accept(new TokenEvent(token));

         this.callback = callback;
         this.skipNulls = skipNulls == null || skipNulls;
     }

    private static <T> BiFunction<JsonParser, TreeNode, T> valueCreator(Class<T> clazz) {
        return (jp, tree) -> {
            try (JsonParser sub = jp.objectReadContext().treeAsTokens(tree)) {
                return sub.readValueAs(clazz);
            } catch (JacksonException e) {
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
                    var currentToken = jp.currentToken();
                    TreeNode tree;
                    if (currentToken == JsonToken.START_OBJECT) {
                        tree = jp.readValueAsTree();
                    } else {
                        tree = null;
                        log.debug("Expected START_OBJECT token but got {}", currentToken);
                    }
                    jp.nextToken();
                    var lastToken = jp.currentToken();

                    this.eventListener.accept(new TokenEvent(lastToken));
                    if (lastToken == JsonToken.END_ARRAY) {
                        tree = null;
                    } else {
                        if (tree instanceof NullNode && skipNulls) {
                            foundNulls++;
                            jp.nextToken();
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
                            eventListener.accept(new NextEvent(next));
                            hasNext = true;
                        }
                        break;
                    } catch (ValueReadException jme) {
                        foundNulls++;
                        logger.warn("{} {} for\n{}\nWill be skipped", jme.getClass(), jme.getMessage(), tree);
                    }
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
    public void close() {
        callback();
        this.jp.close();

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
    @Deprecated
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
        try (JsonGenerator jg = Jackson3Mapper.INSTANCE.mapper().createGenerator(out)) {
            jg.writeStartObject();
            jg.writeArrayPropertyStart("array");
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
        try (JsonGenerator jg = Jackson3Mapper.INSTANCE.mapper().createGenerator(out)) {
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
        final Function<T, Void> logging) {
        while (iterator.hasNext()) {
            T change;
            try {
                change = iterator.next();
                if (change != null) {
                    jg.writePOJO(change);
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

                log.warn("{} {}", e.getClass().getCanonicalName(), e.getMessage());
                jg.writePOJO(e.getMessage());
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

    public static <O> Builder<O> builder() {
        return JsonArrayIterator.<O>_builder();
    }


    public static <O> Builder<O> builder(Class<O> valueClass) {
        return JsonArrayIterator.<O>_builder()
            .valueClass(valueClass);
    }



    public static class ValueReadException extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 6976771876437440576L;

        public ValueReadException(JacksonException e) {
            super(e);
        }
    }

    public class Event {

        public JsonArrayIterator<T> getParent() {
            return JsonArrayIterator.this;
        }
    }

    public class StartEvent extends Event {
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public class TokenEvent extends Event {
        final JsonToken token;

        public TokenEvent(JsonToken token) {
            this.token = token;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public class TotalSizeEvent extends Event {
        final long totalSize;

        public TotalSizeEvent(long totalSize) {
            this.totalSize = totalSize;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public class SizeEvent extends Event {
        final long size;

        public SizeEvent(long size) {
            this.size = size;
        }
    }

    public class NextEvent extends Event {

        final T next;

        public NextEvent(T next) {
            this.next = next;
        }
    }

    @FunctionalInterface
    public interface Listener<S> extends EventListener, Consumer<JsonArrayIterator<S>.Event> {


        static <S> Listener<S> noop() {
            return new Listener<S>() {

                @Override
                public void accept(JsonArrayIterator<S>.Event event) {

                }

                @Override
                public String toString() {
                    return "noop";

                }
            };
        }
    }

}

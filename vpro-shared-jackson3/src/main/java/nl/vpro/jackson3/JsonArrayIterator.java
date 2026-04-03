package nl.vpro.jackson3;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.MismatchedInputException;
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
 * This converts an {@link InputStream} into a Stream of objects, by parsing the stream as JSON.
 * In the simplest case the JSON is just an array, but it can also be an object containing some metadata and an array.
 *
 * @see Builder()
 * @author Michiel Meeuwissen
 * @since 1.0
 */
@Slf4j
public class JsonArrayIterator<T> extends UnmodifiableIterator<T>
    implements CloseableIterator<T>, PeekingIterator<T>, CountedIterator<T> {

    private final JsonParser jp;

    private final ObjectReader reader;

    private T next = null;

    private boolean needsFindNext = true;

    private Boolean hasNext;

    private final BiFunction<ObjectReader, JsonNode, ? extends T> valueCreator;

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

    private boolean skipErrors = true;

    private final Listener<T> eventListener;

    @Getter
    private String property;

    public JsonArrayIterator(InputStream inputStream, Class<T> clazz) {
        this(inputStream, clazz, null);
    }

    public JsonArrayIterator(InputStream inputStream, final Class<T> clazz, Runnable callback) {
        this(inputStream, null, clazz, callback, null, null, null, null, null, null, null, null);
    }

    public JsonArrayIterator(InputStream inputStream, final BiFunction<ObjectReader, JsonNode, T> valueCreator) {
        this(inputStream, valueCreator, null, null, null, null, null, null, null, null, null, null);
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
     * @param inputStream     The {@code InputStream} containing the JSON
     * @param valueCreator    A function which converts a json {@link TreeNode} to the desired objects in the iterator.
     * @param valueClass      If valueCreator is not given, simply the class of the desired object can be given
     *                        JSON unmarshalling with the given objectMapper will happen.
     * @param callback        If the iterator is ready, closed or error this callback will be called.
     * @param property        If specified, then the array will be searched as a value of this property
     * @param sizeField       The size of the iterator, i.e. the size of the array represented in the json stream
     * @param totalSizeField  Sometimes the array is part of something bigger, e.g. a page in a search result. The size
     *                        of the 'complete' result can be in the beginning of the json in this field.
     * @param objectMapper    Default the objectMapper {@link Jackson3Mapper#LENIENT} will be used (in
     *                        conjunction with <code>valueClass</code>, but you may specify another one
     * @param logger          Default this is logging to nl.vpro.jackson2.JsonArrayIterator, but you may override that.
     * @param skipNulls       Whether to skip nulls in the array. Default true.
     * @param skipErrors      Whether to skip objects in the array that can't be marshaled. Default to skipNulls value. If false a {@code null} will be produces (and see skipNulls)
     * @param eventListener   A listener for events that happen during parsing and iteration of the array. See {@link Event} and extension classes.
     * @throws JacksonException    If the JSON parser could not be created or the piece until the start of the array could
     *                        not be tokenized.
     */
    @lombok.Builder(builderClassName = "Builder", builderMethodName = "_builder")
    private JsonArrayIterator(
        @NonNull  InputStream inputStream,
        @Nullable final BiFunction<ObjectReader, JsonNode,  T> valueCreator,
        @Nullable final Class<T> valueClass,
        @Nullable Runnable callback,
        @Nullable String property,
        @Nullable String sizeField,
        @Nullable String totalSizeField,
        @Nullable Jackson3Mapper objectMapper,
        @Nullable Logger logger,
        @Nullable Boolean skipNulls,
        @Nullable Boolean skipErrors,
        @Nullable Listener<T> eventListener
    ) {
        requireNonNull(inputStream, "No inputStream given");
        this.reader = objectMapper == null ? Jackson3Mapper.LENIENT.reader() : objectMapper.reader();
        this.jp = this.reader.createParser(inputStream);
        this.valueCreator = valueCreator == null ? valueCreator(valueClass) : valueCreator;
        if (valueCreator != null && valueClass != null) {
            throw new IllegalArgumentException();
        }
        if (logger != null) {
            this.logger = logger;
        }
        Long tmpSize = null;
        Long tmpTotalSize = null;
        if (sizeField == null) {
            sizeField = "size";
        }
        if (totalSizeField == null) {
            totalSizeField = "totalSize";
        }
        this.eventListener = eventListener;

        String fn = null;
        // find the start of the array, where we will start iterating.
        while(true) {
            JsonToken token = jp.nextToken();
            if (token == null) {
                break;
            }
            eventFor(jp);
            if (token == JsonToken.PROPERTY_NAME) {
                fn = jp.currentName();
            }
            if (token == JsonToken.VALUE_NUMBER_INT && sizeField.equals(fn)) {
                tmpSize = jp.getLongValue();

            }
            if (token == JsonToken.VALUE_NUMBER_INT && totalSizeField.equals(fn)) {
                tmpTotalSize = jp.getLongValue();
            }
            if (token == JsonToken.START_ARRAY) {
                if (property == null || property.equals(fn)) {
                    this.property = fn;
                    break;
                }
            }
        }
        this.size = tmpSize;
        if (this.size != null) {
            event(() -> new SizeEvent(this.size));
        }
        this.totalSize = tmpTotalSize;
        if (this.totalSize != null) {
            event(() -> new TotalSizeEvent(this.totalSize));
        }
        event(StartEvent::new);
        this.callback = callback;
        this.skipNulls = skipNulls == null || skipNulls;
        this.skipErrors = skipErrors == null ||  skipErrors;
    }

    private static <T> BiFunction<ObjectReader, JsonNode, T> valueCreator(Class<T> clazz) {
        return (m, tree) -> m.treeToValue(tree, clazz);

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
                    jp.nextToken();
                    var currentToken = jp.currentToken();

                    if (currentToken == JsonToken.END_ARRAY) {
                        event(() -> new EndEvent(count));
                        eventFor(jp);
                        callback();
                        next = null;
                        hasNext = false;
                        break;
                    }
                    JsonNode tree = jp.readValueAsTree(); // read the next token.
                    if (tree == null) {
                        next = null;
                        hasNext = false;
                        break;
                    }
                    if (tree instanceof NullNode && skipNulls) {
                        foundNulls++;
                        continue;
                    }
                    try {
                        next = valueCreator.apply(reader, tree);
                        event(() -> new NextEvent(next));
                        hasNext = true;
                        break;
                    } catch (MismatchedInputException jme) {
                        foundNulls++;
                        boolean accepted = eventListener != null && eventListener.conditionalAccept(new ValueReadExceptionEvent(tree, jme));
                        if (! accepted) {
                            if (skipNulls) {
                                logger.warn("{} {} for\n{}\nWill be skipped", jme.getClass(), jme.getMessage(), tree);
                            } else {
                                logger.warn("{} {} for\n{}\nWill be null", jme.getClass(), jme.getMessage(), tree);
                            }
                        }
                        if (! skipErrors) {
                            next = null;
                            event(() -> new NextEvent(next));
                            hasNext = true;
                            break;
                        }
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
        if (eventListener != null) {

            try {
                var token = jp.nextToken();
                while (token != null) {
                    eventFor(jp);
                    token = jp.nextToken();
                }
            } catch (Exception e) {
                logger.warn("{} {} while closing JsonParser", e.getClass().getCanonicalName(), e.getMessage());

            }
        }
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
    public void write(OutputStream out, final Consumer<T> logging) {
        write(this, out, logging == null ? null : (c) -> { logging.accept(c); return null;});
    }

    public void writeArray(OutputStream out, final Consumer<T> logging) {
        writeArray(this, out, logging == null ? null : (c) -> { logging.accept(c); return null;});
    }



    /**
     * Write the entire stream to an output stream
     */
    public static <T> void write(
        final CountedIterator<T> iterator,
        final OutputStream out,
        final Function<T, Void> logging) {
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
        final OutputStream out, final Function<T, Void> logging) {
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


    public class Event {

        public JsonArrayIterator<T> getParent() {
            return JsonArrayIterator.this;
        }
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public class StartEvent extends Event {
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    public class EndEvent extends Event {
        private final long totalSize;

    }



    @EqualsAndHashCode(callSuper = true)
    @Data
    public  class TokenEvent extends Event {
        final JsonToken token;

        public TokenEvent(JsonToken token) {
            this.token = token;
        }
    }
    @EqualsAndHashCode(callSuper = true)
    @Data
    public  class PropertyNameEvent extends TokenEvent {
        final String name;

        public PropertyNameEvent(JsonToken token, String name) {
            super(token);
            this.name = name;
        }
    }
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @Data
    public  class ValueEvent<V> extends TokenEvent {
        final V value;

        public ValueEvent(JsonToken token, V value) {
            super(token);
            this.value = value;
        }
    }

    protected void eventFor(JsonParser jp) {
        event(() -> {
            JsonToken token = jp.currentToken();
            return switch (token) {

                case PROPERTY_NAME -> new PropertyNameEvent(jp.currentToken(), jp.currentName());
                case VALUE_NUMBER_INT -> new ValueEvent<>(jp.currentToken(), jp.getIntValue());
                case VALUE_NUMBER_FLOAT -> new ValueEvent<>(jp.currentToken(), jp.getFloatValue());
                case VALUE_EMBEDDED_OBJECT -> new ValueEvent<>(jp.currentToken(), jp.getEmbeddedObject());
                case VALUE_STRING -> new ValueEvent<>(jp.currentToken(), jp.getString());
                case VALUE_TRUE -> new ValueEvent<>(jp.currentToken(), Boolean.TRUE);
                case VALUE_FALSE -> new ValueEvent<>(jp.currentToken(), Boolean.FALSE);
                case VALUE_NULL -> new ValueEvent<Void>(jp.currentToken(), null);
                default -> new TokenEvent(token);
            };
        });
    }
    protected void event(Supplier<Event> event) {
        if (eventListener != null) {
            eventListener.conditionalAccept(event.get());
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


    @EqualsAndHashCode(callSuper = true)
    @Data
    public class NextEvent extends Event {

        final T next;

        public NextEvent(T next) {
            this.next = next;
        }
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public class ValueReadExceptionEvent extends Event {

        final MismatchedInputException exception;

        private final JsonNode json;

        public ValueReadExceptionEvent(JsonNode json, MismatchedInputException exception) {
            this.exception = exception;
            this.json = json;
        }
    }

    @FunctionalInterface
    public interface Listener<S> extends EventListener, Consumer<JsonArrayIterator<S>.Event>  {

        default boolean conditionalAccept(JsonArrayIterator<S>.Event s) {
            accept(s);
            return true;
        }

        static <S> DeafListener<S> noop() {
            return new DeafListener<>();
        }
    }

    public abstract static class ExceptionListener<S> implements Listener<S> {

        public abstract void accept(JsonArrayIterator<S>.ValueReadExceptionEvent s);

        public final  boolean conditionalAccept(JsonArrayIterator<S>.Event s) {
            if (s instanceof JsonArrayIterator<S>.ValueReadExceptionEvent ee) {
                accept(ee);
                return true;
            }
            return false;
        }

        @Override
        public void accept(JsonArrayIterator<S>.Event event) {
            if (event instanceof JsonArrayIterator<S>.ValueReadExceptionEvent ee) {
                accept(ee);
            }
        }

    }

    public static abstract class ConditionalListener<S> implements Listener<S> {

        public void accept(JsonArrayIterator<S>.Event s) {
            conditionalAccept(s);
        }

        public abstract  boolean conditionalAccept(JsonArrayIterator<S>.Event s);

    }

    public static final class DeafListener<S> extends ConditionalListener<S> {


        @Override
        public boolean conditionalAccept(JsonArrayIterator<S>.Event s) {
            return false;
        }
        @Override
        public String toString() {
            return "noop";

        }
    }



}

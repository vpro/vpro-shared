package nl.vpro.jackson2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;

import nl.vpro.util.CloseableIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class JsonArrayIterator<T> extends UnmodifiableIterator<T> implements CloseableIterator<T>, PeekingIterator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(JsonArrayIterator.class);


    final JsonParser jp;

    private T next = null;

    private boolean needsFindNext = true;

    private final Class<T> clazz;

    private final Runnable callback;

    public JsonArrayIterator(InputStream inputStream, Class<T> clazz, Runnable callback) throws IOException {
        this.jp = Jackson2Mapper.getInstance().getFactory().createParser(inputStream);
        this.clazz = clazz;
        while(true) {
            JsonToken token = jp.nextToken();
            if (token == JsonToken.START_ARRAY) break;
        }
        jp.nextToken();
        this.callback = callback;
    }
    @Override
    public boolean hasNext() {
        findNext();
        return next != null;
    }

    @Override
    public T peek() {
        findNext();
        return next;
    }

    @Override
    public T next() {
        findNext();
        T result = next;
        next = null;
        needsFindNext = true;
        return result;
    }
    protected void findNext() {
        if(needsFindNext) {
            while(true) {
                try {
                    TreeNode tree = jp.readValueAsTree();
                    try {
                        next = jp.getCodec().treeToValue(tree, clazz);
                        if (next == null) {
                            if (callback != null) {
                                callback.run();
                            }
                        }
                        break;
                    } catch (JsonMappingException jme) {
                        LOG.error(jme.getClass() + " " + jme.getMessage() + " for\n" + tree + "\nWill be skipped");
                    }
                } catch (IOException e) {
                    if (callback != null) {
                        callback.run();
                    }
                    throw new RuntimeException(e);
                }
            }
            needsFindNext = false;
        }
    }


    @Override
    public void close() throws IOException {
        if (callback != null) {
            callback.run();
        }
        this.jp.close();

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
}

/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.couchdb;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class CouchdbStreamIterator implements Closeable, Iterator<JsonNode> {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final JsonParser parser;

    private final int docDepth;

    private final String docProperty;

    private JsonNode next;

    private int currentDepth = 0;

    private final Closeable closeable;

    protected CouchdbStreamIterator(InputStream stream, int docDepth, String docProperty) throws IOException {
        JsonFactory jsonFactory = mapper.getFactory();
        this.closeable = stream;
        this.parser = jsonFactory.createParser(stream);
        this.parser.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        this.docDepth = docDepth;
        this.docProperty = docProperty;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return next != null;
    }

    @Override
    public JsonNode next() {
        findNext();
        if(next == null) {
            throw new NoSuchElementException();
        }
        JsonNode result = next;
        next = null;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void findNext() {
        if(next == null) {
            try {
                while(true) {
                    JsonToken token = parser.nextToken();
                    if(token == null) {
                        break;
                    }
                    if(token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
                        currentDepth++;
                    }
                    if(token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
                        currentDepth--;
                    }
                    if((token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) && currentDepth == docDepth) {
                        next = parser.readValueAsTree();
                        currentDepth--;
                        if(docProperty.equals(parser.getParsingContext().getParent().getCurrentName())
                            && next.has("doc")
                            && !next.get("doc").has("_attachments")) {
                            break;
                        } else {
                            next = null;
                        }
                    }
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        //this.parser.close(); // Very hard to understand when exactly it closes....
        this.closeable.close();
    }
}

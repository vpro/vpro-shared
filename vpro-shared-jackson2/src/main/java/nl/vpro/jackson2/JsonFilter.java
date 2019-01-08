package nl.vpro.jackson2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class JsonFilter implements Callable<Void> {
    static JsonFactory factory = new JsonFactory();

    static {
        factory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        factory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        factory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        factory.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    final InputStream in;
    final OutputStream out;
    final List<Replacement> replacements;

    public JsonFilter(InputStream in, OutputStream out, List<Replacement> replacements) {
        this.in = in;
        this.out = out;
        this.replacements = replacements;
    }


    @SuppressWarnings("unchecked")
    private <T> T handleReplacements(Deque<String> stack, T value) {
        String fieldName = stack.poll();
        for (Replacement replacement : replacements) {
            if (replacement.key.equals(fieldName)) {
                if (replacement.value.equals(value)) {
                    return (T) replacement.newValue;
                }
            }
        }
        return value;
    }

    @Override
    public Void call() throws IOException {
        final JsonParser parser = factory.createParser(in);
        final JsonGenerator generator = factory.createGenerator(out);
        Deque<String> stack = new ArrayDeque<>();
        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            }
            switch (token) {
                case START_OBJECT:
                    generator.writeStartObject();
                    break;
                case END_OBJECT:
                    generator.writeEndObject();
                    break;
                case START_ARRAY:
                    generator.writeStartArray();
                    break;
                case END_ARRAY:
                    generator.writeEndArray();
                    break;
                case FIELD_NAME: {
                    String fieldName = parser.getText();
                    generator.writeFieldName(fieldName);
                    stack.push(fieldName);
                    break;
                }
                case VALUE_EMBEDDED_OBJECT:
                    //generator.writeObject();
                    stack.poll();
                    break;
                case VALUE_STRING:
                    generator.writeString(handleReplacements(stack, parser.getText()));
                    break;
                case VALUE_NUMBER_INT:
                    generator.writeNumber(handleReplacements(stack, parser.getValueAsInt()));
                    break;
                case VALUE_NUMBER_FLOAT:
                    generator.writeNumber(handleReplacements(stack, parser.getValueAsInt()));
                    break;
                case VALUE_TRUE:
                    generator.writeBoolean(true);
                    stack.poll();
                    break;
                case VALUE_FALSE:
                    generator.writeBoolean(false);
                    stack.poll();
                    break;
                case VALUE_NULL:
                    generator.writeNull();
                    stack.poll();
                    break;

            }
        }
        generator.close();
        return null;
    }
    public static class Replacement<T> {
        private final String key;
        private final T value;
        private final T newValue;

        public Replacement(String key, T value, T newValue) {
            this.key = key;
            this.value = value;
            this.newValue = newValue;
        }

    }
}

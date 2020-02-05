package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;

/**
 * Default Jackson serialized Durations as seconds. In poms we used to serialize durations as Dates, and hence as _milliseconds_.
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class DurationToJsonTimestamp {

    private DurationToJsonTimestamp() {}

    public static class Serializer extends JsonSerializer<Duration> {


        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toMillis());
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<Duration> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
                if (jp.getText().isEmpty() && ctxt.hasDeserializationFeatures(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT.getMask())) {
                    return null;
                } else {
                    return Duration.ofMillis(Long.parseLong(jp.getText()));
                }
            } else {
                return Duration.ofMillis(jp.getLongValue());
            }
        }
    }
}

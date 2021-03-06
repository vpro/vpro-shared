package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Default Jackson serialized Durations as seconds. In poms we used to serialize durations as Dates, and hence as _milliseconds_.
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class DurationToSecondsFloatTimestamp {

    private DurationToSecondsFloatTimestamp() {}

    public static class Serializer extends JsonSerializer<Duration> {


        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toMillis() / 1000f);
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<Duration> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return Duration.ofMillis((long) (Float.parseFloat(jp.getValueAsString()) * 1000));
        }
    }
}

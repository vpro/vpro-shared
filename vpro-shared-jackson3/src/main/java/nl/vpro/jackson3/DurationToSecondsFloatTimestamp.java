package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.*;

import java.time.Duration;

/**
 * Default Jackson serialized Durations as seconds. In poms we used to serialize durations as Dates, and hence as _milliseconds_.
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class DurationToSecondsFloatTimestamp {

    private DurationToSecondsFloatTimestamp() {}

    public static class Serializer extends ValueSerializer<Duration> {


        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toMillis() / 1000f);
            }
        }
    }


    public static class Deserializer extends ValueDeserializer<Duration> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext ctxt) {
            return Duration.ofMillis((long) (Float.parseFloat(jp.getValueAsString()) * 1000));
        }
    }
}

package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.*;

import java.time.Duration;
import java.util.Calendar;

import nl.vpro.util.TimeUtils;

/**
 * Default Jackson serialized Durations as seconds. In poms we used to serialize durations as Dates, and hence as _milliseconds_.
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class DurationToJsonTimestamp {

    private DurationToJsonTimestamp() {}

    public static class Serializer extends ValueSerializer<Duration> {


        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.toMillis());
            }
        }

    }

    public static class XmlSerializer extends ValueSerializer<javax.xml.datatype.Duration> {


        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(javax.xml.datatype.Duration value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(value.getTimeInMillis(Calendar.getInstance()));
            }
        }
    }


    public static class Deserializer extends ValueDeserializer<Duration> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext ctxt) {
            if (jp.currentToken() == JsonToken.VALUE_STRING) {
                if (jp.getString().isEmpty() && ctxt.hasDeserializationFeatures(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT.getMask())) {
                    return null;
                } else {
                    return TimeUtils.parseDuration(jp.getString()).orElseThrow();
                }
            } else {
                return Duration.ofMillis(jp.getLongValue());
            }
        }
    }
}

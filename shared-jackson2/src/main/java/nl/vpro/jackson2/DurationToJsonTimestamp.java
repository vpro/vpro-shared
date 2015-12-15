package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author Michiel Meeuwissen
 * @since 0.31
 */
public class DurationToJsonTimestamp {

    public static class Serializer extends JsonSerializer<Duration> {


        public static Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeNumber(TimeUnit.MILLISECONDS.convert(value.get(ChronoUnit.SECONDS), TimeUnit.SECONDS));
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<Duration> {

        public static Deserializer INSTANCE = new Deserializer();
        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return Duration.of(jp.getLongValue(), ChronoUnit.MILLIS);
        }
    }
}

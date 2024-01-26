package nl.vpro.jackson2;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import nl.vpro.util.TimeUtils;


/**
 * @since 2.0
 */
public class LocalDateTimeToJsonDateWithSpace {

    private LocalDateTimeToJsonDateWithSpace() {}

    public static class Serializer extends JsonSerializer<LocalDateTime> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(value.toString().replaceFirst("T", " "));
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<LocalDateTime> {

        public static final Deserializer INSTANCE = new Deserializer();

        @Override
        public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            String text = jp.getText();
            if (text == null) {
                return null;
            } else {
                try {
                    return LocalDateTime.parse(text.replaceFirst(" ", "T"));
                } catch (DateTimeParseException dtf) {
                    return TimeUtils.parse(text).map(i -> i.atZone(TimeUtils.ZONE_ID).toLocalDateTime()).orElseThrow(() -> new IOException("Cannot parse " + text));
                }
            }
        }
    }
}

package nl.vpro.jackson3;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import tools.jackson.core.*;
import tools.jackson.databind.*;

import nl.vpro.util.TimeUtils;


/**
 * @since 2.0
 */
public class LocalDateTimeToJsonDateWithSpace {

    private LocalDateTimeToJsonDateWithSpace() {}

    public static class Serializer extends ValueSerializer<LocalDateTime> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(LocalDateTime value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(value.toString().replaceFirst("T", " "));
            }
        }


    }

    public static class Deserializer extends ValueDeserializer<LocalDateTime> {

        public static final Deserializer INSTANCE = new Deserializer();

        @Override
        public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) {
            String text = jp.getString();
            if (text == null) {
                return null;
            } else {
                try {
                    return LocalDateTime.parse(text.replaceFirst(" ", "T"));
                } catch (DateTimeParseException dtf) {
                    return TimeUtils.parse(text).map(i -> i.atZone(TimeUtils.ZONE_ID).toLocalDateTime()).orElseThrow(() -> JacksonException.wrapWithPath(dtf, "Cannot parse " + text));
                }
            }
        }
    }
}

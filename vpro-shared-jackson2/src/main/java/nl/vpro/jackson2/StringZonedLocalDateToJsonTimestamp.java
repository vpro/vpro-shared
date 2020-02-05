package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import nl.vpro.util.BindingUtils;

import static nl.vpro.jackson2.DateModule.ZONE;


/**
 * @since 2.5
 */
public class StringZonedLocalDateToJsonTimestamp {

    private StringZonedLocalDateToJsonTimestamp() {}

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-ddZZZZZ")
        .withLocale(Locale.US);


    public static class Serializer extends JsonSerializer<Object> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                if (value instanceof CharSequence) {
                    value = LocalDate.parse(value.toString().substring(0, 10));
                }
                jgen.writeNumber(((LocalDate) value).atStartOfDay(BindingUtils.DEFAULT_ZONE).toInstant().toEpochMilli());
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<Object> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            try {
                return Instant.ofEpochMilli(jp.getLongValue()).atZone(ZONE).toLocalDate();
            } catch (JsonParseException jps) {
                String s = jp.getValueAsString();
                if (s == null) {
                    return null;
                }
                return ZonedDateTime.parse(jp.getValueAsString(), FORMATTER).toLocalDate();
            }
        }
    }
}

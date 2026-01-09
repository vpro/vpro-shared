package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import nl.vpro.util.BindingUtils;
import nl.vpro.util.TimeUtils;

import static nl.vpro.jackson3.DateModule.ZONE;


/**
 * @since 2.5
 */
public class StringZonedLocalDateToJsonTimestamp {

    private StringZonedLocalDateToJsonTimestamp() {}

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-ddZZZZZ")
        .withLocale(Locale.US);


    public static class Serializer extends ValueSerializer<Object> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
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

    public static class Deserializer extends ValueDeserializer<Object> {

        public static final Deserializer INSTANCE = new Deserializer();
        @Override
        public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt)  {
            try {
                return Instant.ofEpochMilli(jp.getLongValue()).atZone(ZONE).toLocalDate();
            } catch (JacksonException jps) {
                String s = jp.getValueAsString();
                if (s == null) {
                    return null;
                }
                return TimeUtils.parseLocalDate(s).orElseThrow();
                //return ZonedDateTime.parse(jp.getValueAsString(), FORMATTER).toLocalDate();
            }
        }
    }
}

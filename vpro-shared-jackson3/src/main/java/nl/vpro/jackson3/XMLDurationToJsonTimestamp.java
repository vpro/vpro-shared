package nl.vpro.jackson3;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.*;
import tools.jackson.databind.*;

import java.util.*;

import javax.xml.datatype.*;

import nl.vpro.util.TimeUtils;

@Slf4j
public class XMLDurationToJsonTimestamp {


    public static class Serializer extends ValueSerializer<Duration> {

        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(0);
            jgen.writeNumber(value.getTimeInMillis(cal));
        }
    }

    public static class DeserializerDate extends ValueDeserializer<Date> {
        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt)  {
            return new Date(jp.getLongValue());
        }
    }

    public static class Deserializer extends ValueDeserializer<Duration> {
        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext ctxt) {
            DatatypeFactory datatypeFactory;
            try {
                datatypeFactory = DatatypeFactory.newInstance();
                return datatypeFactory.newDuration(jp.getLongValue());
            } catch (DatatypeConfigurationException e) {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    }

    public static class SerializerString extends ValueSerializer<String> {

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            java.time.Duration duration = TimeUtils.parseDuration(value).orElse(null);
            if (duration != null) {
                jgen.writeNumber(duration.toMillis());
            } else {
                jgen.writeNull();
            }
        }

    }

    public static class DeserializerJavaDuration extends ValueDeserializer<java.time.Duration> {
        @Override
        public java.time.Duration deserialize(JsonParser jp, DeserializationContext ctxt)  {
            return java.time.Duration.ofMillis(jp.getLongValue());
        }
    }
}

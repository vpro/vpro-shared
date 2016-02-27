package nl.vpro.jackson2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class XMLDurationToJsonTimestamp {

    public static class Serializer extends JsonSerializer<Duration> {

        @Override
        public void serialize(Duration value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(0);
            jgen.writeNumber(value.getTimeInMillis(cal));
        }
    }

    public static class DeserializerDate extends JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return new Date(jp.getLongValue());
        }
    }

    public static class Deserializer extends JsonDeserializer<Duration> {
        @Override
        public Duration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            DatatypeFactory datatypeFactory = null;
            try {
                datatypeFactory = DatatypeFactory.newInstance();
                return datatypeFactory.newDuration(jp.getLongValue());
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}

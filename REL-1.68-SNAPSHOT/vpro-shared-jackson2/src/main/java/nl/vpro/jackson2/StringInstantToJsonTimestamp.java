/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Instant;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * These can be used in conjuction with InstantXmlAdapter, if you want 'millis since epoch' in JSON, but formatted date stamps in xml.
 * (this is what we normally do)
 * @author Michiel Meeuwissen
 * @since 0.39
 */
public class StringInstantToJsonTimestamp {

    private static final Logger LOG = LoggerFactory.getLogger(StringInstantToJsonTimestamp.class);

    public static class Serializer extends JsonSerializer<String> {
        public static StringInstantToJsonTimestamp.Serializer INSTANCE = new StringInstantToJsonTimestamp.Serializer();

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                try {
                    jgen.writeNumber(DatatypeConverter.parseTime(value).toInstant().toEpochMilli());
                } catch (IllegalArgumentException iae) {
                    LOG.warn("Could not parse {}. Writing null to json", value);
                    jgen.writeNull();
                }
            }
        }
    }


    public static class Deserializer extends JsonDeserializer<Instant> {

        public static StringInstantToJsonTimestamp.Deserializer INSTANCE = new StringInstantToJsonTimestamp.Deserializer();

        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            switch(jp.getCurrentTokenId()) {
                case JsonTokenId.ID_NUMBER_INT:
                    return Instant.ofEpochMilli(jp.getLongValue());
                case JsonTokenId.ID_NULL:
                    return null;
                case JsonTokenId.ID_STRING:
                    try {
                        return DatatypeConverter.parseTime(jp.getText()).toInstant();
                    } catch (IllegalArgumentException iae) {
                        LOG.warn("Could not parse {}. Writing null to json", jp.getText());
                        return null;
                    }
                default:
                    LOG.warn("Could not parse {} to instant. Returing null", jp.toString());
                    return null;
            }
        }
    }
}

/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson2;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

/**
 * These can be used in conjuction with InstantXmlAdapter, if you want 'millis since epoch' in JSON, but formatted date stamps in xml.
 * (this is what we normally do)
 * @author Michiel Meeuwissen
 * @since 0.39
 */
@Slf4j
public class StringInstantToJsonTimestamp {

    private StringInstantToJsonTimestamp() {}

    public static class Serializer extends JsonSerializer<Object> {
        public static final StringInstantToJsonTimestamp.Serializer INSTANCE = new StringInstantToJsonTimestamp.Serializer();

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else if (value instanceof Instant) { // if no JaxbAnnotationIntrospector
                jgen.writeNumber(((Instant) value).toEpochMilli());
            } else if (value instanceof CharSequence) {
                try {
                    jgen.writeNumber(parseDateTime(String.valueOf(value)).toEpochMilli());
                } catch (IllegalArgumentException iae) {
                    log.warn("Could not parse {}. Writing null to json", value);
                    jgen.writeNull();
                }
            }
        }
    }

    static Instant parseDateTime(String value) {
        try {
            long asLong = Long.parseLong(value);
            return Instant.ofEpochMilli(asLong);
        } catch (NumberFormatException ignore) {
        }
        try {
            return DatatypeConverter.parseTime(value).toInstant();
        } catch (IllegalArgumentException iae) {
            try {

                Optional<Instant> natty = NattySupport.parseDate(value);
                if (natty.isPresent()) {
                    return natty.get();
                }
            } catch (Throwable e) {
                log.debug("Natty couldn't parse {}: {}", value, e.getMessage());
            }
            throw iae;
        }
    }

    public static class Deserializer extends JsonDeserializer<Instant> {

        public static final StringInstantToJsonTimestamp.Deserializer INSTANCE = new StringInstantToJsonTimestamp.Deserializer();

        @Override
        public Instant deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            switch(jp.getCurrentTokenId()) {
                case JsonTokenId.ID_NUMBER_INT:
                    return Instant.ofEpochMilli(jp.getLongValue());
                case JsonTokenId.ID_NULL:
                    return null;
                case JsonTokenId.ID_STRING:
                    try {
                        return parseDateTime(jp.getText());
                    } catch (IllegalArgumentException iae) {
                        log.warn("Could not parse {}. Writing null to json", jp.getText());
                        return null;
                    }
                default:
                    log.warn("Could not parse {} to instant. Returning null", jp.toString());
                    return null;
            }
        }
    }
}

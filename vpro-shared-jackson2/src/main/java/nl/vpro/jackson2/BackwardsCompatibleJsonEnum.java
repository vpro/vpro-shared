package nl.vpro.jackson2;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

/**
 * Newer jackson version suddenly recognized @XmlEnumValue. This makes it possible to fall back to old behaviour.
 * {@link nl.vpro.domain.media.support.Workflow}
 */
@Slf4j
public class BackwardsCompatibleJsonEnum {


    public static class Serializer extends JsonSerializer<Enum> {

        @Override
        public void serialize(Enum value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(value.name());
            }
        }
    }

    public static abstract class Deserializer<T extends Enum<T>> extends JsonDeserializer<T> {
        final Class<T> enumClass;

        public Deserializer(Class<T> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            try {
                return Enum.valueOf(enumClass, jp.getValueAsString());
            } catch(IllegalArgumentException iae) {
                try {
                    return Enum.valueOf(enumClass, jp.getValueAsString().toUpperCase());
                } catch (IllegalArgumentException iaeu) {
                    if (ctxt.getConfig().hasDeserializationFeatures(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL.getMask())) {
                        return null;
                    } else {
                        throw iae;
                    }
                }

            }
        }

    }
}

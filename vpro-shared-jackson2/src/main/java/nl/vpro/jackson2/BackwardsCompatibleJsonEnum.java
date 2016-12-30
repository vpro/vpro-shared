package nl.vpro.jackson2;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Newer jackson version suddenly recognized @XmlEnumValue. This makes it possible to fall back to old behaviour.
 * {@link nl.vpro.domain.media.support.Workflow}
 */
public class BackwardsCompatibleJsonEnum {

    private static final Logger LOG = LoggerFactory.getLogger(BackwardsCompatibleJsonEnum.class);


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
            ctxt.getConfig().getTypeFactory();
            return Enum.valueOf(enumClass, jp.getValueAsString());
        }

    }
}

package nl.vpro.jackson3;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;

/**
 * Newer jackson version suddenly recognized @XmlEnumValue. This makes it possible to fall back to old behaviour.
 * {@code nl.vpro.domain.media.support.Workflow}
 */
@Slf4j
public class BackwardsCompatibleJsonEnum {


    public static class Serializer extends ValueSerializer<Enum<?>> {

        @Override
        public void serialize(Enum<?> value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
            if (value == null) {
                jgen.writeNull();
            } else {
                jgen.writeString(value.name());
            }
        }

    }

    public static abstract class Deserializer<T extends Enum<T>> extends ValueDeserializer<T> {
        final Class<T> enumClass;

        public Deserializer(Class<T> enumClass) {
            this.enumClass = enumClass;
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt) {
            try {
                return Enum.valueOf(enumClass, jp.getValueAsString());
            } catch(IllegalArgumentException iae) {
                try {
                    return Enum.valueOf(enumClass, jp.getValueAsString().toUpperCase());
                } catch (IllegalArgumentException iaeu) {
                    if (ctxt.getConfig().hasDeserializationFeatures(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL. getMask())) {
                        return null;
                    } else {
                        throw iae;
                    }
                }

            }
        }

    }
}

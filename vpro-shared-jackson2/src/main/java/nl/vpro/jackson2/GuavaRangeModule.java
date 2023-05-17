package nl.vpro.jackson2;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public class GuavaRangeModule extends SimpleModule {

    private static final long serialVersionUID = -8048846883670339246L;

    public GuavaRangeModule() {
        super(new Version(3, 5, 0, "", "nl.vpro.shared", "vpro-jackson2-guavarange"));
        addSerializer( Serializer.INSTANCE);
        addDeserializer(Range.class, new Deserializer<>(Instant.class));
        addDeserializer(Range.class, new Deserializer<>(Integer.class));
        addDeserializer(Range.class, new Deserializer<>(String.class));
        addDeserializer(Range.class, new Deserializer<>(Instant.class));



    }

    public static class Serializer extends com.fasterxml.jackson.databind.ser.std.StdSerializer<Range<?>> {

        private static final long serialVersionUID = -4394016847732058088L;
        public static Serializer INSTANCE = new Serializer();

        protected Serializer() {
            super(new CollectionLikeType(
                SimpleType.constructUnsafe(Range.class),
                SimpleType.constructUnsafe(Comparable.class)) {
                private static final long serialVersionUID = -2803462566784593946L;
            });
        }

        @Override
        public void serialize(Range<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeStartObject();
                if (value.hasLowerBound()) {
                    gen.writeObjectField("lowerEndpoint", value.lowerEndpoint());
                    gen.writeObjectField("lowerBoundType", value.lowerBoundType());
                }
                if (value.hasUpperBound()) {
                    gen.writeObjectField("upperEndpoint", value.upperEndpoint());
                    gen.writeObjectField("upperBoundType", value.upperBoundType());
                }
                gen.writeEndObject();
            }
        }
    }

    public static class Deserializer<T extends Comparable<T>> extends StdDeserializer<Range<T>> {

        private static final long serialVersionUID = -4394016847732058088L;

        private final Class<T> clazz;

        protected Deserializer(Class<T> comparable) {
            super(new CollectionLikeType(
                SimpleType.constructUnsafe(Range.class),
                SimpleType.constructUnsafe(comparable)) {
                private static final long serialVersionUID = -2803462566784593946L;
            });
            this.clazz = comparable;
        }

        @Override
        public Range<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            return null;
        }

        @Override
        public Range<T> deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException {

            JsonNode node = ctxt.readValue(p, JsonNode.class);

            if (node.has("lowerEndpoint")) {
                BoundType type = BoundType.valueOf(node.get("lowerBoundType").asText());
                return Range.downTo(p.readValueAs(clazz),  type);
            }
            return null;

        }
    }
}

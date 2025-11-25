package nl.vpro.jackson3;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.Serial;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public class GuavaRangeModule extends SimpleModule {

    public static final String LOWER_ENDPOINT = "lowerEndpoint";
    public static final String LOWER_BOUND_TYPE = "lowerBoundType";
    public static final String UPPER_ENDPOINT = "upperEndpoint";
    public static final String UPPER_BOUND_TYPE = "upperBoundType";

    @Serial
    private static final long serialVersionUID = -8048846883670339246L;

    public GuavaRangeModule() {
        super(new Version(3, 5, 0, "", "nl.vpro.shared", "vpro-jackson2-guavarange"));
        addSerializer( Serializer.INSTANCE);
        addDeserializer(Range.class, Deserializer.INSTANCE);
    }

    public static class Serializer extends com.fasterxml.jackson.databind.ser.std.StdSerializer<Range<?>> {

        @Serial
        private static final long serialVersionUID = -4394016847732058088L;
        public static Serializer INSTANCE = new Serializer();

        protected Serializer() {
            super(new CollectionLikeType(
                SimpleType.constructUnsafe(Range.class),
                SimpleType.constructUnsafe(Comparable.class)) {
                @Serial
                private static final long serialVersionUID = -2803462566784593946L;
            });
        }

        @Override
        public void serialize(Range<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeStartObject();
                Class<?> type = null;

                if (value.hasLowerBound()) {
                    type = value.lowerEndpoint().getClass();
                    gen.writeObjectField(LOWER_ENDPOINT, value.lowerEndpoint());
                    gen.writeObjectField(LOWER_BOUND_TYPE, value.lowerBoundType());
                }
                if (value.hasUpperBound()) {
                    type = value.upperEndpoint().getClass();
                    gen.writeObjectField(UPPER_ENDPOINT, value.upperEndpoint());
                    gen.writeObjectField(UPPER_BOUND_TYPE, value.upperBoundType());
                }
                if (type != null) {
                    gen.writeObjectField("type", type.getName());
                }
                gen.writeEndObject();
            }
        }
    }

    public static class Deserializer extends StdDeserializer<Range<?>> {

        @Serial
        private static final long serialVersionUID = -4394016847732058088L;
        public static Deserializer INSTANCE = new Deserializer();


        protected Deserializer() {
            super(new CollectionLikeType(
                SimpleType.constructUnsafe(Range.class),
                SimpleType.constructUnsafe(Comparable.class)) {
                @Serial
                private static final long serialVersionUID = -2803462566784593946L;
            });
        }



        @SneakyThrows
        @Override
        public Range<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

            if (p.currentToken() == JsonToken.START_OBJECT) {
                p.nextToken();
            }
            JsonNode node = ctxt.readValue(p, JsonNode.class);
            if (node.has("type")) {
                Class type = Class.forName(node.get("type").textValue());
                return of(type, p, node);
            } else {
                return Range.all();
            }

        }
    }

    static <C extends Comparable<C>> Range<C> of(Class<C> clazz, JsonParser p, JsonNode node) throws IOException {

        BoundType lowerBoundType = null;
        C lowerValue = null;
        BoundType upperBoundType = null;
        C upperValue = null;

        if (node.has(LOWER_ENDPOINT)) {
            lowerBoundType = BoundType.valueOf(node.get(LOWER_BOUND_TYPE).asText());
            lowerValue = p.getCodec().treeToValue(node.get(LOWER_ENDPOINT), clazz);
        }
        if (node.has(UPPER_ENDPOINT)) {
            upperBoundType = BoundType.valueOf(node.get(UPPER_BOUND_TYPE).asText());
            upperValue = p.getCodec().treeToValue(node.get(UPPER_ENDPOINT), clazz);
        }
        if (lowerValue != null) {
            if (upperValue != null) {
                return Range.range(lowerValue, lowerBoundType, upperValue, upperBoundType);
            } else {
                return Range.downTo(lowerValue, lowerBoundType);
            }
        } else {
            if (upperValue != null) {
                return Range.upTo(upperValue, upperBoundType);
            } else {
                return Range.all();
            }
        }
    }



}

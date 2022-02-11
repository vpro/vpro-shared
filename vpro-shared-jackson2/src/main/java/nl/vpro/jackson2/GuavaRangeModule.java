package nl.vpro.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.google.common.collect.Range;

public class GuavaRangeModule extends SimpleModule {

    private static final long serialVersionUID = -8048846883670339246L;

    public GuavaRangeModule() {
        super(new Version(1, 29, 1, "", "nl.vpro.shared", "vpro-jackson2"));
        addSerializer(Serializer.INSTANCE);
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
}

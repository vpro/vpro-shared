package nl.vpro.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Range;

public class GuavaRange {

    public static class Serializer extends JsonSerializer<Range<?>> {

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

package nl.vpro.jackson2;

import java.io.IOException;

import org.apache.avro.specific.SpecificRecordBase;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class Avro {

    public static class Serializer extends JsonSerializer<SpecificRecordBase> {


        public static Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(SpecificRecordBase value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeRaw(value.toString());
            }

        }
    }


}

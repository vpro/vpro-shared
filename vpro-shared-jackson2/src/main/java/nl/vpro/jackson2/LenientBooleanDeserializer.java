package nl.vpro.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public class LenientBooleanDeserializer extends JsonDeserializer<Boolean> {

    public static final LenientBooleanDeserializer INSTANCE = new LenientBooleanDeserializer();

    private LenientBooleanDeserializer() {

    }


    @Override
    public Boolean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonToken token = jsonParser.getCurrentToken();
        if (jsonParser.isNaN()) {
            return false;
        }
        if (token.isBoolean()) {
            return jsonParser.getBooleanValue();
        } else if (token.isNumeric()) {
            return jsonParser.getNumberValue().longValue() != 0;
        } else {
            String text = jsonParser.getText().toLowerCase();
            switch(text) {
                case "true":
                case "1":
                    return true;
                default:
                    return false;
            }
        }

    }
}

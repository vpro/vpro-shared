package nl.vpro.jackson3;

import tools.jackson.core.*;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public class LenientBooleanDeserializer extends ValueDeserializer<Boolean> {

    public static final LenientBooleanDeserializer INSTANCE = new LenientBooleanDeserializer();

    private LenientBooleanDeserializer() {

    }


    @Override
    public Boolean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        JsonToken token = jsonParser.currentToken();
        if (jsonParser.isNaN()) {
            return false;
        }
        if (token.isBoolean()) {
            return jsonParser.getBooleanValue();
        } else if (token.isNumeric()) {
            return jsonParser.getNumberValue().longValue() != 0;
        } else {
            String text = jsonParser.getString().toLowerCase();
            return switch (text) {
                case "true", "1" -> true;
                default -> false;
            };
        }

    }
}

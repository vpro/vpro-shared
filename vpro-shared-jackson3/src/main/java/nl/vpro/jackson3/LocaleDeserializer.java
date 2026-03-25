package nl.vpro.jackson3;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.Locale;

/**
 *  This LocaleDserializer honor {@link DeserializationFeature#ACCEPT_EMPTY_STRING_AS_NULL_OBJECT}
 */
public class LocaleDeserializer extends StdDeserializer<Locale>   {


    protected LocaleDeserializer() {
        super(Locale.class);
    }

    @Override
    public Locale deserialize(JsonParser p, DeserializationContext ctxt)  {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getString();
            if (s.isEmpty() && ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                return null;
            }
            s = s.trim();

            // try language tag first, fallback to language-only constructor
            try {
                return Locale.forLanguageTag(s);
            } catch (Exception e) {
                return new Locale(s);
            }
        } else if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        // Unexpected token - delegate to context to throw an informative exception
        return (Locale) ctxt.handleUnexpectedToken(_valueClass, p);
    }
}

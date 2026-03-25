package nl.vpro.jackson2;

import java.io.IOException;
import java.io.Serial;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * This LocaleDserializer honor {@link DeserializationFeature#ACCEPT_EMPTY_STRING_AS_NULL_OBJECT}
 */
public class LocaleDeserializer extends StdDeserializer<Locale> {

    @Serial
    private static final long serialVersionUID = 7661781034564971827L;

    public LocaleDeserializer() {
        super(Locale.class);
    }

    @Override
    public Locale deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getText();
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

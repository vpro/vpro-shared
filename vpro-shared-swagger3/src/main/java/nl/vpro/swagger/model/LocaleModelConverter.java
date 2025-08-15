package nl.vpro.swagger.model;

import io.swagger.v3.core.converter.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Locale;

import com.fasterxml.jackson.databind.type.SimpleType;

/**
 *
 */
public class LocaleModelConverter implements ModelConverter {

    @SuppressWarnings({"rawtypes"})
    @Override
    public Schema resolve(
        AnnotatedType type,
        ModelConverterContext context,
        Iterator<ModelConverter> chain) {
        Type t = type.getType();
        if (t instanceof SimpleType) {
            SimpleType simpleType = (SimpleType) t;
            if (Locale.class.isAssignableFrom(simpleType.getRawClass())) {
                StringSchema stringSchema = new StringSchema();

                // TODO we may just generate a regexp matching all languages
                stringSchema.setPattern("[a-z]{2}(:?_[A-Z]{2})?");
                return stringSchema;
            }
        }
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;

    }
}

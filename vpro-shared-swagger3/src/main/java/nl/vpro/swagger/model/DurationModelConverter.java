package nl.vpro.swagger.model;

import io.swagger.v3.core.converter.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Iterator;

import com.fasterxml.jackson.databind.type.SimpleType;

/**
 *
 */
public class DurationModelConverter implements ModelConverter {

    @SuppressWarnings({"rawtypes"})
    @Override
    public Schema resolve(
        AnnotatedType type,
        ModelConverterContext context,
        Iterator<ModelConverter> chain) {
        Type t = type.getType();
        if (t instanceof SimpleType simpleType) {
            if (Duration.class.isAssignableFrom(simpleType.getRawClass())) {
                StringSchema stringSchema = new StringSchema();
                // actuallyu, for json it's millis, for xml it's Duration#toString

                return stringSchema;
            }
        }
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;

    }
}

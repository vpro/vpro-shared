package nl.vpro.swagger.model;

import io.swagger.v3.core.converter.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.lang.reflect.Type;
import java.util.Iterator;

import com.fasterxml.jackson.databind.type.SimpleType;

/**
 * I think it's a bit ridiculous that it's not working default like this.
 *<p>
 * I also think it is silly that openapi doesn't support something basic like htmls <option value="key">displayable string</input>
 * <p>
 * <a href="https://github.com/springdoc/springdoc-openapi/issues/1247">issue 1247</a>
 * <a href="https://github.com/springdoc/springdoc-openapi/issues/2048">issue 2048</a>
 */
public class EnumModelConverter implements ModelConverter {

	@SuppressWarnings({"rawtypes", "unchecked"})
    @Override
	public Schema resolve(
        AnnotatedType type,
        ModelConverterContext context,
        Iterator<ModelConverter> chain) {
        Type t = type.getType();
        if (t instanceof SimpleType) {
            SimpleType simpleType = (SimpleType) t;
            if (simpleType.getRawClass().isEnum()) {
                Class<Enum<?>> enumClass = (Class<Enum<?>>) simpleType.getRawClass();
                StringSchema stringSchema = new StringSchema();
                Enum<?>[] enumConstants = enumClass.getEnumConstants();
                for (Enum<?> en : enumConstants) {
                    String enumValue = en.name();
                    stringSchema.addEnumItem(enumValue);
                }
                return stringSchema;
            }
        }
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;

	}
}

package nl.vpro.jackson2.rs;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * Sometimes jackson/resteasy will not unmarshal a json because there is no type information, but the prototype actually specifies it fully. This message body reader will deal with that (using {@link TypeIdResolver#idFromBaseType()}, by adding the id implicitly (if it is missing) before the actual unmarshal.
 *
 * @author Michiel Meeuwissen
 * @since 2.7
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
@Priority(JacksonContextResolver.PRIORITY - 1) // It must be a bit higher than that, so that it goes before
public class JsonIdAdderBodyReader implements MessageBodyReader<Object> {

    @Context
    Providers providers;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public Object readFrom(
        final @NonNull Class<Object> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders,
        InputStream entityStream) throws WebApplicationException, IOException {
        ObjectMapper mapper =  providers == null ? null : providers.getContextResolver(ObjectMapper.class,  MediaType.APPLICATION_JSON_TYPE).getContext(type);
        if (mapper == null) {
            log.info("No mapper found in {}", providers);
            mapper = Jackson2Mapper.getLenientInstance();
        }
        final JavaType javaType = mapper.getTypeFactory().constructType(genericType);
        final JsonNode jsonNode = mapper.readTree(entityStream);
        if (jsonNode instanceof ObjectNode) {
            final ObjectNode objectNode = (ObjectNode) jsonNode;
            final TypeDeserializer typeDeserializer = mapper.getDeserializationConfig().findTypeDeserializer(javaType);
            if (typeDeserializer != null) {
                final String propertyName = typeDeserializer.getPropertyName();
                final String propertyValue = typeDeserializer.getTypeIdResolver().idFromBaseType();
                if (! objectNode.has(propertyName)) {
                    log.debug("Implicitly setting {} = {} for {}", propertyName, propertyValue, javaType);
                    objectNode.put(propertyName, propertyValue);
                }
            }
        }
        return mapper.treeToValue(jsonNode, javaType.getRawClass());


    }
}

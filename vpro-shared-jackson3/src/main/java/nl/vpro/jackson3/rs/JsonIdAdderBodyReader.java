package nl.vpro.jackson3.rs;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;

import org.checkerframework.checker.nullness.qual.NonNull;

import nl.vpro.jackson3.Jackson3Mapper;

/**
 * Sometimes jackson/resteasy will not unmarshal a json because there is no type information, but the prototype actually specifies it fully. This message body reader will deal with that (using {@link TypeIdResolver#idFromBaseType(tools.jackson.databind.DatabindContext)}, by adding the id implicitly (if it is missing) before the actual unmarshal.
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

    @SneakyThrows
    @Override
    public Object readFrom(
        final @NonNull Class<Object> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders,
        InputStream entityStream) throws WebApplicationException {

        final ObjectReader reader;
        JsonMapper mapper =  providers == null ? null : providers.getContextResolver(JsonMapper.class,  MediaType.APPLICATION_JSON_TYPE).getContext(type);
        if (mapper == null) {
            mapper = Jackson3Mapper.LENIENT.mapper();
        }
        reader = mapper.reader();
        ObjectReader objectReader = reader.forType(type);
        final JsonParser parser = reader.createParser(entityStream);

        final JsonNode jsonNode = parser.readValueAsTree();
        if (jsonNode instanceof ObjectNode objectNode) {

            final JavaType javaType = reader.typeFactory().constructType(genericType);
            final TypeDeserializer typeDeserializer = mapper
                .deserializationConfig()
                .getTypeResolverProvider()
                .findTypeDeserializer(null, javaType,  null);
            if (typeDeserializer != null) {
                final String propertyName = typeDeserializer.getPropertyName();
                final String propertyValue = typeDeserializer.getTypeIdResolver().idFromBaseType(null);
                if (! objectNode.has(propertyName)) {
                    log.debug("Implicitly setting {} = {} for {}", propertyName, propertyValue, javaType);
                    objectNode.put(propertyName, propertyValue);
                }
            }
        }
        return mapper.treeToValue(jsonNode, type);


     }
}

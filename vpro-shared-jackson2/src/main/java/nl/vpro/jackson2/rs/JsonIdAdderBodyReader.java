package nl.vpro.jackson2.rs;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * Sometimes resteasy will not unmarshal an jsoin because there is not type information, but the prototype actully specifies it fully. The message body reader will deal with that, by adding the id implicetely (if it is missing) before the actual unmarshall.
 *
 * @author Michiel Meeuwissen
 * @since 2.7
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class JsonIdAdderBodyReader implements MessageBodyReader<Object> {


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);

    }

    @Override
    public Object readFrom(
        Class<Object> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders,
        InputStream entityStream) throws WebApplicationException, IOException {
        Jackson2Mapper mapper = Jackson2Mapper.getLenientInstance(); // TODO somehow get object mapper from resteasy context
        JavaType javaType = mapper.getTypeFactory().constructType(genericType);
        JsonNode jsonNode = mapper.readTree(entityStream);
        if (jsonNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            TypeDeserializer typeDeserializer = mapper.getDeserializationConfig().findTypeDeserializer(javaType);
            if (typeDeserializer != null) {
                String propertyName = typeDeserializer.getPropertyName();
                String propertyValue = typeDeserializer.getTypeIdResolver().idFromBaseType();
                if (! objectNode.has(propertyName)) {
                    log.debug("Implicetely setting {} = {} for {}", propertyName, propertyValue, javaType);
                    objectNode.put(propertyName, propertyValue);
                }
            }
        }
        return mapper.treeToValue(jsonNode, javaType.getRawClass());


    }
}

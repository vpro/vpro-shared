package nl.vpro.rs.client;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import jakarta.ws.rs.ext.ContextResolver;

import nl.vpro.jackson3.Jackson3Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class JacksonContextResolver extends JacksonXmlBindJsonProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonContextResolver() {
        this(null);
    }
    public JacksonContextResolver(ObjectMapper mapper) {
        this.mapper = mapper == null ? Jackson3Mapper.LENIENT.mapper() : mapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return mapper;
    }
}


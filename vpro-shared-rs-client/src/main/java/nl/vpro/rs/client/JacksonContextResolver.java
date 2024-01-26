package nl.vpro.rs.client;

import jakarta.ws.rs.ext.ContextResolver;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class JacksonContextResolver extends JacksonXmlBindJsonProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonContextResolver() {
        this(Jackson2Mapper.getLenientInstance());
    }
    public JacksonContextResolver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return mapper == null ? Jackson2Mapper.getLenientInstance() : mapper;
    }
}


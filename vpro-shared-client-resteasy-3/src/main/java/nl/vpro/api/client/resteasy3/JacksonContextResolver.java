package nl.vpro.api.client.resteasy3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import nl.vpro.jackson2.Jackson2Mapper;

import javax.ws.rs.ext.ContextResolver;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
class JacksonContextResolver extends JacksonJaxbJsonProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonContextResolver() {
        this(Jackson2Mapper.LENIENT);
    }
    public JacksonContextResolver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return mapper == null ? Jackson2Mapper.LENIENT : mapper;
    }
}


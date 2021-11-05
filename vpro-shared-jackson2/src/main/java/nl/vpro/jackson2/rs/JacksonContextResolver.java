package nl.vpro.jackson2.rs;

import javax.annotation.Priority;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "application/*+json", "text/json"})
@Produces({MediaType.APPLICATION_JSON, "application/*+json", "text/json"})
@Priority(JacksonContextResolver.PRIORITY)
public class JacksonContextResolver extends JacksonJaxbJsonProvider implements ContextResolver<ObjectMapper> {

    static final int PRIORITY = Priorities.USER;

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


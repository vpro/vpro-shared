package nl.vpro.jackson2.rs;

import javax.annotation.Priority;
import javax.ws.rs.*;
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
@Consumes({"application/json", "application/*+json", "text/json"})
@Produces({"application/json", "application/*+json", "text/json"})
@Priority(Priorities.USER)
public class JacksonContextResolver extends JacksonJaxbJsonProvider implements ContextResolver<ObjectMapper> {

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


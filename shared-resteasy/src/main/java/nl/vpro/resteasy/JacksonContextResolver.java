package nl.vpro.resteasy;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
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
@Consumes({"application/*+json", "text/json"})
@Produces({"application/*+json", "text/json"})
public class JacksonContextResolver extends JacksonJaxbJsonProvider implements ContextResolver<ObjectMapper> {

    public JacksonContextResolver() throws Exception {
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return Jackson2Mapper.INSTANCE;
    }
}


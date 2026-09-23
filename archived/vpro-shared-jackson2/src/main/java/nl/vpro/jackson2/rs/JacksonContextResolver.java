package nl.vpro.jackson2.rs;

import jakarta.annotation.Priority;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * This is used to bind our object mapper to resteasy/jaxrs.
 * <p>
 * The exposed mapper is actually in a thread local. Some interceptor may influence it.
 *
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "application/*+json", "text/json"})
@Produces({MediaType.APPLICATION_JSON, "application/*+json", "text/json"})
@Priority(JacksonContextResolver.PRIORITY)
public class JacksonContextResolver extends JacksonXmlBindJsonProvider implements ContextResolver<ObjectMapper> {

    static final int PRIORITY = Priorities.USER;

    private final ThreadLocal<ObjectMapper> mapper;

    public JacksonContextResolver() {
        this(Jackson2Mapper.getLenientInstance());
    }
    public JacksonContextResolver(ObjectMapper mapper) {
        this(() -> mapper);
    }

    public JacksonContextResolver(Supplier<ObjectMapper> mapper) {
        this.mapper = ThreadLocal.withInitial(mapper);
    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return mapper.get();
    }

    /**
     * @since 4.0
     */
    public void set(ObjectMapper mapper) {
        this.mapper.set(mapper);
    }

    /**
     * @since 4.0
     */
    public void reset() {
        mapper.remove();
    }
}


package nl.vpro.jackson3.rs;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import nl.vpro.jackson3.Jackson3Mapper;

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
public class JacksonContextResolver extends JacksonXmlBindJsonProvider implements ContextResolver<JsonMapper> {

    static final int PRIORITY = Priorities.USER;

    private final ThreadLocal<Jackson3Mapper> mapper;

    public JacksonContextResolver() {
        this(Jackson3Mapper.LENIENT);
    }
    public JacksonContextResolver(Jackson3Mapper mapper) {
        this(() -> mapper);
    }

    public JacksonContextResolver(Supplier<Jackson3Mapper> mapper) {
        this.mapper = ThreadLocal.withInitial(mapper);
    }

    @Override
    public JsonMapper getContext(Class<?> objectType) {
        return mapper.get().mapper();
    }

    /**
     * @since 4.0
     */
    public void set(Jackson3Mapper mapper) {
        this.mapper.set(mapper);
    }

    /**
     * @since 4.0
     */
    public void reset() {
        mapper.remove();
    }
}


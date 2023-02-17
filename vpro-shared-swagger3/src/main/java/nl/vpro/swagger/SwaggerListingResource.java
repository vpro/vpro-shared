package nl.vpro.swagger;


import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;


/**
 *
 * @author Michiel Meeuwissen
 * @since 1.60
 */
@Path("/openapi") // we use to arrange content negotation with headers, and via resteasy.media.type.mappings in web.xml (to support .json, .yaml too)
@Slf4j
public class SwaggerListingResource extends BaseOpenApiResource  {

    @Context
    ServletConfig config;

    @Context
    Application app;

    OpenAPI api;
    OpenApiContext ctx;
    boolean pretty;


    @Inject
    public SwaggerListingResource(OpenAPIConfiguration openAPIConfiguration, OpenApiContext openApiContext, OpenAPI api) {
        setOpenApiConfiguration(openAPIConfiguration);
        this.ctx = openApiContext;
        this.api = api;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo) throws Exception {

        OpenAPI oas = getOpenAPI(config, app);
        if (oas == null) {
            return Response.status(404).build();
        }

        // something with filter here, which I dropped, because currently not used
        if (headers.getAcceptableMediaTypes().get(0).isCompatible(MediaType.valueOf("application/yaml"))) {
            return Response.status(Response.Status.OK)
                    .entity(pretty ?
                            ctx.getOutputYamlMapper().writer(new DefaultPrettyPrinter()).writeValueAsString(oas) :
                            ctx.getOutputYamlMapper().writeValueAsString(oas))
                    .type("application/yaml")
                    .build();
        } else {
            return Response.status(Response.Status.OK)
                    .entity(pretty ?
                            ctx.getOutputJsonMapper().writer(new DefaultPrettyPrinter()).writeValueAsString(oas) :
                            ctx.getOutputJsonMapper().writeValueAsString(oas))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    synchronized OpenAPI getOpenAPI(ServletConfig config,Application app) throws OpenApiConfigurationException {
        if (api == null) {
            String ctxId = getContextId(config);
            ctx = new JaxrsOpenApiContextBuilder()
                .servletConfig(config)
                .application(app)
                .resourcePackages(resourcePackages)
                .configLocation(configLocation)
                .openApiConfiguration(openApiConfiguration)
                .ctxId(ctxId)
                .buildContext(true);
            api = ctx.read();
            pretty = ctx.getOpenApiConfiguration() != null &&
                Boolean.TRUE.equals(ctx.getOpenApiConfiguration().isPrettyPrint());
        }
        return api;
    }


}

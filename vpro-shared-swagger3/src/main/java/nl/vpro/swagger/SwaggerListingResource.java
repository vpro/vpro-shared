package nl.vpro.swagger;


import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import nl.vpro.rs.ResteasyApplication;


/**
 *
 * @author Michiel Meeuwissen
 * @since 1.60
 */
@Path("/openapi") // we use to arrange content negotation with headers, and via resteasy.media.type.mappings in web.xml (to support .json, .yaml too)
@Slf4j
public class SwaggerListingResource extends BaseOpenApiResource  {

    OpenAPI api;
    OpenApiContext ctx;
    boolean pretty;

    @Inject
    public SwaggerListingResource(OpenAPIConfiguration openAPIConfiguration, OpenApiContext openApiContext, OpenAPI api) {
        setOpenApiConfiguration(openAPIConfiguration);
        this.ctx = openApiContext;
        this.api = api;
        this.pretty = openAPIConfiguration.isPrettyPrint();
    }

    @PostConstruct
    public void inject() {
        ResteasyApplication.inject(this);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    public Response getOpenApi(
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo) throws Exception {

        OpenAPI oas = getOpenAPI();
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

    synchronized OpenAPI getOpenAPI() {
        return api;
    }


}

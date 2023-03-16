package nl.vpro.swagger;


import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author Michiel Meeuwissen
 * @since 1.60
 */
@Path("/openapi") // we use to arrange content negotation with headers, and via resteasy.media.type.mappings in web.xml (to support .json, .yaml too)
@Slf4j
public class SwaggerListingResource extends BaseOpenApiResource  {

    Provider<OpenAPI> api;
    Provider<OpenApiContext> ctx;
    Provider<OpenAPIConfiguration> openApiConfiguration;

    @Inject
    public SwaggerListingResource(
        Provider<OpenAPIConfiguration> openAPIConfiguration,
        Provider<OpenApiContext> openApiContext,
        Provider<OpenAPI> api) {
        this.ctx = openApiContext;
        this.api = api;
        this.openApiConfiguration = openAPIConfiguration;
    }

    @PostConstruct
    public void inject() {
        //ResteasyApplication.inject(this);
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

        boolean pretty = openApiConfiguration.get().isPrettyPrint();
        // something with filter here, which I dropped, because currently not used
        if (headers.getAcceptableMediaTypes().get(0).isCompatible(MediaType.valueOf("application/yaml"))) {
            return Response.status(Response.Status.OK)
                    .entity(pretty ?
                            ctx.get().getOutputYamlMapper().writer(new DefaultPrettyPrinter()).writeValueAsString(oas) :
                            ctx.get().getOutputYamlMapper().writeValueAsString(oas))
                    .type("application/yaml")
                    .build();
        } else {
            return Response.status(Response.Status.OK)
                    .entity(pretty ?
                            ctx.get().getOutputJsonMapper().writer(new DefaultPrettyPrinter()).writeValueAsString(oas) :
                            ctx.get().getOutputJsonMapper().writeValueAsString(oas))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    synchronized OpenAPI getOpenAPI() {
        return api.get();
    }


}

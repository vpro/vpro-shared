package nl.vpro.swagger;


import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;


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

        if (isYaml(headers)) {
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

    private static final MediaType YAML = new MediaType("application", "yaml");

    /**
     * (Reluctantly) match to yaml.
     */
    static boolean isYaml(HttpHeaders headers) {
        return isYaml(headers.getAcceptableMediaTypes());
    }

     /**
     * (Reluctantly) match to yaml.
     */
    public  static boolean isYaml(List<MediaType> acceptable) {
        boolean yaml = false;
        for (MediaType type : acceptable) {
            if (YAML.getType().equals(type.getType()) && YAML.getSubtype().equals(type.getSubtype())) {
                yaml = true;
                break;
            }

        }
        return yaml;
    }

    synchronized OpenAPI getOpenAPI() {
        return api.get();
    }


}

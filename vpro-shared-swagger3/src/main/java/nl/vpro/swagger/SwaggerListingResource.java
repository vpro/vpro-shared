package nl.vpro.swagger;


import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;


/**
 * The provided implementations by swagger don't work or give in log:
 * 2016-12-09 10:51:13,933 WARN - 0:0:0:0:0:0:0:1  GET /api/swagger.json RESTEASY002142: Multiple resource methods match request "GET /swagger". Selecting one. Matching methods: [public javax.ws.rs.core.Response io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.getListingJson(javax.ws.rs.core.Application,javax.servlet.ServletConfig,javax.ws.rs.core.HttpHeaders,javax.ws.rs.core.UriInfo), public javax.ws.rs.core.Response io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.getListingYaml(javax.ws.rs.core.Application,javax.servlet.ServletConfig,javax.ws.rs.core.HttpHeaders,javax.ws.rs.core.UriInfo)]  [ org.jboss.resteasy.resteasy_jaxrs.i18n - http-nio-8070-exec-1 ]
 * <p>
 * Probably caused by setting in our web.xml, but anyway.
 *
 * @author Michiel Meeuwissen
 * @since 1.60
 */
@Path("/openapi")
public class SwaggerListingResource extends OpenApiResource {

    @Context
    ServletContext context;



    @Inject
    public SwaggerListingResource(OpenAPIConfiguration openAPIConfiguration) {
        setOpenApiConfiguration(openAPIConfiguration);

    }


}

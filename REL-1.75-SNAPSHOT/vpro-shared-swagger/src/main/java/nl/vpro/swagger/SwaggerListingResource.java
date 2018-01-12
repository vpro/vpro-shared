package nl.vpro.swagger;


import io.swagger.annotations.ApiOperation;
import io.swagger.jaxrs.listing.BaseApiListingResource;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import com.fasterxml.jackson.core.JsonProcessingException;


/**
 * The provided implementations by swagger don't work or give in log:
 * 2016-12-09 10:51:13,933 WARN - 0:0:0:0:0:0:0:1  GET /api/swagger.json RESTEASY002142: Multiple resource methods match request "GET /swagger". Selecting one. Matching methods: [public javax.ws.rs.core.Response io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.getListingJson(javax.ws.rs.core.Application,javax.servlet.ServletConfig,javax.ws.rs.core.HttpHeaders,javax.ws.rs.core.UriInfo), public javax.ws.rs.core.Response io.swagger.jaxrs.listing.AcceptHeaderApiListingResource.getListingYaml(javax.ws.rs.core.Application,javax.servlet.ServletConfig,javax.ws.rs.core.HttpHeaders,javax.ws.rs.core.UriInfo)]  [ org.jboss.resteasy.resteasy_jaxrs.i18n - http-nio-8070-exec-1 ]
 *
 * Probably caused by setting in our web.xml, but anyway.
 *
 * @author Michiel Meeuwissen
 * @since 1.60
 */

@Path("/swagger")
public class SwaggerListingResource extends BaseApiListingResource {

    @Context
    ServletContext context;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "The swagger definition in JSON", hidden = true)
    public Response getListingJson(
        @Context Application app,
        @Context ServletConfig sc,
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo) throws JsonProcessingException {
        return getListingJsonResponse(app, context, sc, headers, uriInfo);
    }
}

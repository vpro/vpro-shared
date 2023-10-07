package nl.vpro.rs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
@Path("/version")
@Produces(MediaType.TEXT_PLAIN)
public interface VersionRestService {


    @GET
    String getVersion();
}

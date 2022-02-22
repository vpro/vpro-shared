package nl.vpro.rs.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Ernst Bunders
 */
@Provider
public class ServerErrorExceptionMapper extends ApiErrorMapper implements ExceptionMapper<ServerErrorException> {

    @Override
    public Response toResponse(ServerErrorException exception) {
        return createResponse(exception.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
}

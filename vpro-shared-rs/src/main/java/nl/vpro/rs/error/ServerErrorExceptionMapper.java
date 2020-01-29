package nl.vpro.rs.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Date: 23-4-12
 * Time: 10:52
 *
 * @author Ernst Bunders
 */
@Provider
public class ServerErrorExceptionMapper extends ApiErrorMapper implements ExceptionMapper<ServerErrorException> {

    @Override
    public Response toResponse(ServerErrorException exception) {
        return createResponse(exception.getMessage(), 500);
    }
}

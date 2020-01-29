package nl.vpro.rs.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Ernst Bunders
 */
@Provider
public class NotFoundExceptionMapper extends ApiErrorMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        return createResponse(exception.getMessage(), 404);
    }
}

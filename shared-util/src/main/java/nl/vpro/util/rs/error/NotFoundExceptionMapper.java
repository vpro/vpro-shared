package nl.vpro.util.rs.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Date: 23-4-12
 * Time: 10:36
 *
 * @author Ernst Bunders
 */
@Provider
public class NotFoundExceptionMapper extends ApiErrorMapper implements ExceptionMapper<NotFoundException> {

    public Response toResponse(NotFoundException exception) {
        return createResponse(exception.getMessage(), 404);
    }
}

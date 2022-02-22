package nl.vpro.rs.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Ernst Bunders
 */
@Provider
public class DataErrorExceptionMapper extends ApiErrorMapper implements ExceptionMapper<DataErrorException> {

    @Override
    public Response toResponse(DataErrorException exception) {
        return createResponse(exception.getDataError(), Response.Status.INTERNAL_SERVER_ERROR, exception.getMessage());
    }
}

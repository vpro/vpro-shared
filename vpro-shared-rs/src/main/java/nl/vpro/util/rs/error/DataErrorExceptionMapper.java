package nl.vpro.util.rs.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Date: 2-5-12
 * Time: 14:23
 *
 * @author Ernst Bunders
 */
@Provider
public class DataErrorExceptionMapper extends ApiErrorMapper implements ExceptionMapper<DataErrorException> {

    @Override
    public Response toResponse(DataErrorException exception) {
        return createResponse(exception.getDataError(), 500, exception.getMessage());
    }
}

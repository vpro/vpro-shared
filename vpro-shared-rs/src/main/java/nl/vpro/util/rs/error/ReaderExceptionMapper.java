package nl.vpro.util.rs.error;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ReaderException;


/**
 *
 * @author Ernst Bunders
 */
@Provider
public class ReaderExceptionMapper extends ApiErrorMapper implements ExceptionMapper<ReaderException> {

    @Override
    public Response toResponse(ReaderException exception) {
        return createResponse("Something went wrong unmarshalling the data. reason: " + exception.getMessage(), 500);
    }
}

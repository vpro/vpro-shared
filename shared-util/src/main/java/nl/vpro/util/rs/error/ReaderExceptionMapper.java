package nl.vpro.util.rs.error;

import org.jboss.resteasy.spi.ReaderException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Date: 2-5-12
 * Time: 15:16
 *
 * @author Ernst Bunders
 */
@Provider
public class ReaderExceptionMapper extends ApiErrorMapper implements ExceptionMapper<ReaderException> {

    @Override
    public Response toResponse(ReaderException exception) {
        return createResponse("Something went wrong unmarshalling the data. reason: "+exception.getMessage() , 500);
    }
}

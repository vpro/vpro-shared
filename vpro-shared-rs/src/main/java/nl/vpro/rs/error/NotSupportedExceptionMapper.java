package nl.vpro.rs.error;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Provider
public class NotSupportedExceptionMapper extends ApiErrorMapper implements ExceptionMapper<NotSupportedException> {
    @Override
    public Response toResponse(NotSupportedException exception) {
        return createResponse(exception.getMessage(), 415);

    }
}

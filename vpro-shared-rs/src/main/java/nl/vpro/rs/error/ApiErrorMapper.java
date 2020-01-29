package nl.vpro.rs.error;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import nl.vpro.rs.transfer.ErrorResponse;

/**
 * This class is ment to catch exceptions thrown in the process of (un)marshalling domain objects bij RestEasy.

 *
 * @author Ernst Bunders
 */
public abstract class ApiErrorMapper {
    protected Response createResponse(String message, int status) {
        return Response
            .status(status)
            .entity(new ErrorResponse(message, status))
            .build();
    }

    protected Response createResponse(DataError error, int status, String message) {
        ErrorResponse errorResponse = new ErrorResponse(error.getDescription(), status);
        errorResponse.setDataError(error);
        if (StringUtils.isNotBlank(message)) {
            errorResponse.setMessage(message);
        }
        return Response
            .status(status)
            .entity(errorResponse)
            .build();
    }
}

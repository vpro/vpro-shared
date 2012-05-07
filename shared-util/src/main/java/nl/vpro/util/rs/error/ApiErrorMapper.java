package nl.vpro.util.rs.error;

import nl.vpro.util.rs.transfer.ErrorResponse;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.Response;

/**
 * This class is ment to catch exceptions thrown in the process of (un)marshalling domain objects bij RestEasy.
 * Date: 23-4-12
 * Time: 10:48
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

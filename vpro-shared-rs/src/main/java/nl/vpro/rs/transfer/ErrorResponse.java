package nl.vpro.rs.transfer;

import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.*;

import com.google.common.base.MoreObjects;

import nl.vpro.rs.error.DataError;

/**

 * @author Ernst Bunders
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class ErrorResponse {

    private Integer status;

    private String message;

    private DataError dataError;

    public ErrorResponse() {
    }

    public ErrorResponse(String message, Response.Status status) {
        this.message = message;
        this.status = status.getStatusCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("status", status)
            .add("message", message)
            .add("dataError", dataError)
            .toString();
    }
}

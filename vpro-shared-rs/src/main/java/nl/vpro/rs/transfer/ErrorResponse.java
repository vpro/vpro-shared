package nl.vpro.rs.transfer;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

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

    public ErrorResponse(String message, Integer status) {
        this.message = message;
        this.status = status;
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

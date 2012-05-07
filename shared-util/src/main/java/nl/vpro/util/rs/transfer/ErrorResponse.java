package nl.vpro.util.rs.transfer;

import nl.vpro.util.rs.error.DataError;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Date: 23-4-12
 * Time: 10:45
 *
 * @author Ernst Bunders
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ErrorResponse {

    private Integer status;
    private String message;
    private DataError dataError;

    public ErrorResponse() { }

    public ErrorResponse(String message, Integer status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public Integer getStatus() {
        return status;
    }

    public DataError getDataError() {
        return dataError;
    }

    public void setDataError(DataError dataError) {
        this.dataError = dataError;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

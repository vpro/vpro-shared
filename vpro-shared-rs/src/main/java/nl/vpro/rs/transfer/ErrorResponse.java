package nl.vpro.rs.transfer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import nl.vpro.rs.error.DataError;

/**
 * Date: 23-4-12
 * Time: 10:45
 *
 * @author Ernst Bunders
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
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

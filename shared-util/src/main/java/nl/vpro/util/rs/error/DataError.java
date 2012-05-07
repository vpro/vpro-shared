package nl.vpro.util.rs.error;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Date: 2-5-12
 * Time: 14:12
 *
 * @author Ernst Bunders
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public enum DataError {
    NODATA(0, "The request contains no data."),
    INVALIDDATA(1, "The request contains invalid data that could not be parsed"),
    NOID(2, "You can not update a document with no id"),
    DELETED(3, "You can not update a document that has been deleted before."),
    EXPIRED(4, "You can not update an exired version of the document"),
    ILLEGALSTATE(5, "The data contains a field value that should not be there");

    @XmlAttribute(name = "code")
    private int errorCode;
    @XmlAttribute(name = "description")
    private String description;

    private DataError(int errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getDescription() {
        return description;
    }
}

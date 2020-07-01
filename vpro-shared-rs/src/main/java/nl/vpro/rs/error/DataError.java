package nl.vpro.rs.error;


import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ernst Bunders
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public enum DataError {
    NODATA(0, "The request contains no data."),
    INVALIDDATA(1, "The request contains invalid data that could not be parsed"),
    NOID(2, "You can not update a document with no id"),
    DELETED(3, "You can not update a document that has been deleted before."),
    EXPIRED(4, "You can not update an exired version of the document"),
    ILLEGALSTATE(5, "The data contains a field value that should not be there");

    @XmlAttribute(name = "code")
    @Getter
    private final int errorCode;
    @XmlAttribute(name = "description")
    @Getter
    private final String description;

    DataError(int errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }

    @Override
    public String toString() {
        return name() + ":" + errorCode + ":" + description;
    }

}

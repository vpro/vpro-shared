package nl.vpro.test.util.jaxb.test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class C {
    @XmlValue
    String value = "cc";
}

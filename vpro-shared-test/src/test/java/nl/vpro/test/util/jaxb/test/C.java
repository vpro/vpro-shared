package nl.vpro.test.util.jaxb.test;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

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

package nl.vpro.test.util.jaxb.test;


import lombok.Getter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@XmlRootElement(namespace = "")
public class ANoNamespace {

    @XmlElement(namespace = "")
    String a = "xx";

    @XmlElement(namespace = "")
    String b = "yy";

    @XmlElement(namespace = "")
    String c = "zz";
}

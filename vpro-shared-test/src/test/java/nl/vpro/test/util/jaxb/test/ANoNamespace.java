package nl.vpro.test.util.jaxb.test;


import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

package nl.vpro.test.util.jaxb.test;

import lombok.Getter;

import javax.xml.bind.annotation.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@XmlRootElement
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public class A {
    String value = "aa";

    B b = new B();
}


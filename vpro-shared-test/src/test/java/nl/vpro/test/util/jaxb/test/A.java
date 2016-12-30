package nl.vpro.test.util.jaxb.test;

import javax.xml.bind.annotation.*;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@XmlRootElement
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class A {
    String value = "aa";

    B b = new B();
}


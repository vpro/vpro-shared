package nl.vpro.test.util.jaxb.test;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * This has no XmlRootElement
 * @author Michiel Meeuwissen
 * @since ...
 */
@XmlType(name = "bType")
@XmlAccessorType(XmlAccessType.FIELD)
public class B {
    String value = "bb";


    @XmlAttribute
    Integer i = 1;


    @XmlAttribute
    Integer j = 2;

    C c = new C();
}

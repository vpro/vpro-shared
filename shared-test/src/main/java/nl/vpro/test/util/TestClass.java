package nl.vpro.test.util;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * @author Michiel Meeuwissen
 * @since 0.36
 */
@XmlType
@XmlRootElement
public class TestClass<T> {
    @XmlValue
    public T value;

    public TestClass(T v) {
        this.value = v;
    }

    public TestClass() {

    }


}

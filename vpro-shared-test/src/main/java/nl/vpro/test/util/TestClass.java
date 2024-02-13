package nl.vpro.test.util;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestClass<?> testClass = (TestClass<?>) o;

        return value != null ? value.equals(testClass.value) : testClass.value == null;

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}

package nl.vpro.elasticsearch.highlevel;

import com.google.common.base.MoreObjects;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class A {
    public String id = String.valueOf(ExtendedElasticSearchIteratorITest.ID++);
    public String title = "bar";
    public int value;

    public A() {

    }

    public A(String title, int value) {
        this.title = title;
        this.value = value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("title", title)
            .add("value", value)
            .toString();
    }
}

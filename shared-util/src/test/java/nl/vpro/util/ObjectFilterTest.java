package nl.vpro.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.59
 */
public class ObjectFilterTest {

    public static class A {
        private String field1 = "a";
        private B field2 = new B(this);

    }

    public static class B {
        A parent;
        private String field = "b";
        public B() {

        }
        public B(A parent) {
            this.parent = parent;
        }
    }
    @Test
    public void test() {
        A a1 = new A();
        a1.field1 = "aa";
        A a2 = new A();
        a2.field2.field = "c";

        List<A> col = new ArrayList<>(Arrays.asList(a1, a2));

        ObjectFilter.Result<List<A>> result = ObjectFilter.filter(col, (o) -> true);
        assertThat(result.filterCount()).isEqualTo(1);
        List<A> clone = result.get();
        assertThat(clone.get(0).field1).isEqualTo("aa");
        assertThat(clone.get(0).field2.field).isEqualTo("b");
        assertThat(clone.get(0).field2.parent.field1).isEqualTo("aa");
        assertThat(clone.get(1).field1).isEqualTo("a");
        assertThat(clone.get(1).field2.field).isEqualTo("c");
    }

}

package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 */
public class ReflectionUtilsTest {

    public enum AnEnum {
        X,Y,Z;

    }
    @Getter @Setter
    public static class Base {
        private String  a = "A";

        private List<String> list;

        protected Base(String a, List<String> list) {
            this.a = a;
            this.list = list;
        }
        protected Base() {

        }

    }

    @Getter @Setter
    public static class A extends Base {
        private Integer b;
        private AnEnum  e;


        public A() {
        }
    }


    public static class AWithOutSetters {
        public String  a = "A";
        public Integer b;
        public AnEnum  e;


        public AWithOutSetters() {
        }
    }




    public static class AWithBuilder extends Base{
        private Integer b;
        private AnEnum  e;

        @lombok.Builder(builderClassName = "Builder")
        public AWithBuilder(String a, Integer b, AnEnum e, List<String> list) {
            super(a, list);
            this.b = b;
            this.e = e;

        }
    }

    public static Map<String, String> properties = new HashMap<>();
    static {
        properties.put("a", "B");
        properties.put("b", "3");
        properties.put("e", "y");
        properties.put("list", "foo,bar");


    }

    @Test
    public void testOnlyIfNull() {
        AWithOutSetters a = new AWithOutSetters();
        ReflectionUtils.configureIfNull(a, properties);
        assertThat(a.a).isEqualTo("A");
        assertThat(a.b).isEqualTo(3);

    }


    @Test
    public void testOnField() {
        AWithOutSetters a = new AWithOutSetters();
        ReflectionUtils.configured(a, properties);
        assertThat(a.a).isEqualTo("B");
        assertThat(a.b).isEqualTo(3);

    }

    @Test
    public void testOnMethods() {
        A a = new A();
        ReflectionUtils.configured(a, properties);
        assertThat(a.getA()).isEqualTo("B");
        assertThat(a.b).isEqualTo(3);
        assertThat(a.e).isEqualTo(AnEnum.Y);


    }

    @Test
    public void testConfigureIfNullBuilder() {
        AWithBuilder.Builder builder = AWithBuilder
            .builder()
            .a("A")
            ;
        ReflectionUtils.configureIfNull(builder, properties);
        AWithBuilder a = builder.build();
        assertThat(a.getA()).isEqualTo("A");
        assertThat(a.b).isEqualTo(3);
        assertThat(a.e).isEqualTo(AnEnum.Y);

    }
    @Test
    public void testConfigureBuilder() {
        AWithBuilder.Builder builder = AWithBuilder
            .builder()
            .a("A")
            ;
        ReflectionUtils.configured(builder, properties);
        AWithBuilder a = builder.build();
        assertThat(a.getA()).isEqualTo("B");
        assertThat(a.b).isEqualTo(3);
        assertThat(a.e).isEqualTo(AnEnum.Y);
    }


    @Test
    public void testConfigureInstance() {
        A a = ReflectionUtils.configured(A.class, properties);
        assertThat(a.getA()).isEqualTo("B");
        assertThat(a.b).isEqualTo(3);
        assertThat(a.e).isEqualTo(AnEnum.Y);
    }

    @Test
    public void testConfigureInstanceWithBuilder() {
        AWithBuilder a = ReflectionUtils.configured(AWithBuilder.class, properties);
        assertThat(a.getA()).isEqualTo("B");
        assertThat(a.b).isEqualTo(3);
        assertThat(a.e).isEqualTo(AnEnum.Y);
        assertThat(a.getList()).containsExactly("foo", "bar");

    }

}

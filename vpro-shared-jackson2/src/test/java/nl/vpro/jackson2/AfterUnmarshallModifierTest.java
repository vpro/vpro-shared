package nl.vpro.jackson2;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 2.17
 */
@Slf4j
class AfterUnmarshallModifierTest {


    public static class MyClass {


        @JsonProperty
        A a;

        @JsonProperty
        B b;

        @JsonCreator
        MyClass(
            @JsonProperty("a") A a, @JsonProperty("b") B b) {
            this.a =a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "My";
        }
    }

    public static class A {

        @JsonBackReference
        B parent;

        @Override
        public String toString() {
            return "a";
        }
    }

    public static class B extends A {

        @JsonManagedReference
        List<A> as;



        @Override
        public String toString() {
            return "b" + as;
        }

    }

    @Test
    public void benchmark() throws JsonProcessingException {
        MyClass my = Jackson2Mapper.getLenientInstance().readValue("{'a': {}}", MyClass.class);
       for (long i = 0 ; i < 1000000L; i++) {
            my = Jackson2Mapper.getLenientInstance().readValue("{'a': {}}", MyClass.class);
        }
        long nano = System.nanoTime();
        for (long i = 0 ; i < 1000000L; i++) {
            my = Jackson2Mapper.getLenientInstance().readValue("{'a': {}}", MyClass.class);
        }
        log.info("{}", Duration.ofNanos(System.nanoTime() - nano));
        assertThat(my.a.parent).isEqualTo(my);
    }

    @Test
    public void modifier() throws JsonProcessingException {
        MyClass my = Jackson2Mapper.getLenientInstance().readValue("{'a': {}, 'b' : {'as': [{}, {}]}}", MyClass.class);

        assertThat(my.a.parent).isEqualTo(my);
        assertThat(my.b.parent).isEqualTo(my);
        assertThat(my.b.as.get(0).parent).isEqualTo(my.b);

    }

    @Test
    public void modifierSimple() throws JsonProcessingException {
        B my = Jackson2Mapper.getLenientInstance().readValue("{'as': [{}, {}]}", B.class);
        assertThat(my.as.get(0).parent).isEqualTo(my);

    }


}

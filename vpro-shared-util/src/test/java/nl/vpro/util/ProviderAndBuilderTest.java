package nl.vpro.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;

import javax.inject.Provider;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class ProviderAndBuilderTest {

    @AllArgsConstructor
    @Data
    @Builder
    public static class A {
        private String a;
        private Integer b;

    }
    @Data
    public static class AProvider implements Provider<A> {

        private Optional<String> a;
        private Integer b;

        @Override
        public A get() {
            return ProviderAndBuilder
                .fillAndCatch(this, A.builder()).build();
        }
    }

    @Test
    public void build() throws Exception {
        AProvider a = new AProvider();
        a.setA(Optional.of("X"));
        a.setB(8);


        A built = a.get();

        assertThat(built.getA()).isEqualTo("X");
        assertThat(built.getB()).isEqualTo(8);
    }


    @Test
    public void buildNull() throws Exception {
        AProvider a = new AProvider();
        a.setA(Optional.empty());
        a.setB(null);


        A built = a.get();

        assertThat(built.getA()).isNull();
        assertThat(built.getB()).isNull();
    }
}

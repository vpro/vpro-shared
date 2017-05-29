package nl.vpro.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
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
        private Duration duration;
        private Duration anotherDuration;

    }
    @Data
    public static class AProvider implements Provider<A> {

        @Named("foo.bar") @Inject
        // This would be the way for guice to optionally inject
        private Optional<String> a;
        private Integer b;
        private Duration duration;
        private Optional<String> anotherDuration;

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
        a.setAnotherDuration(Optional.of("10s"));


        A built = a.get();

        assertThat(built.getA()).isEqualTo("X");
        assertThat(built.getB()).isEqualTo(8);
        assertThat(built.getAnotherDuration()).isEqualTo(Duration.ofSeconds(10));
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

    @Test
    public void buildDuration() throws Exception {
        AProvider a = new AProvider();
        a.setDuration(TimeUtils.parseDuration("5s").orElse(null));
        a.setB(null);


        A built = a.get();

        assertThat(built.getDuration()).isEqualTo(Duration.ofSeconds(5));
    }
}

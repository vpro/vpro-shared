package nl.vpro.guice;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import nl.vpro.util.DefaultValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class DurationConvertorAndOptionalModuleTest {

    public static class B {

    }

    public static class C {

    }

    public static class D {

    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class A {
        @Inject
        @Named("duration")
        public Duration duration;

        @Inject
        @Named("optionalduration")
        @DefaultValue("0.1S")
        public Optional<Duration> optionalDuration;

        @Inject
        @Named("optionaldurationwithoutdefault")
        public Optional<Duration> optionalDurationWithoutDefault;

        final Duration constructorArgument;

        @Inject
        Optional<B> b;

        @Inject
        C c;

        @Inject
        Optional<D> d;

        @Inject
        public A(@Named("constructorArgument") Optional<Duration> duration) {
            this.constructorArgument = duration.orElse(null);
        }
    }

    private Injector injector;
    @Before
    public void setup() {

        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    Map<String, String> properties1 = new HashMap<>();
                    properties1.put("duration", "20S");
                    Names.bindProperties(binder(), properties1);
                    binder().bind(D.class).toInstance(new D());
                }
            },
            new Convertors(),
            new OptionalModule(A.class)
        );



    }

    @Test
    public void test() {
        A a = injector.getInstance(A.class);
        assertThat(a.duration).isEqualTo(Duration.ofSeconds(20));
        assertThat(a.optionalDuration.get()).isEqualTo(Duration.ofMillis(100));
        assertThat(a.optionalDurationWithoutDefault.isPresent()).isFalse();
        assertThat(a.constructorArgument).isNull();
        assertThat(a.b.isPresent()).isFalse();
        assertThat(a.c).isNotNull();
        assertThat(a.d.isPresent()).isTrue();


    }


}

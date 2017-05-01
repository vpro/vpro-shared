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
public class DurationConvertorTest {

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


    }


}

package nl.vpro.guice;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class DurationConvertorTest {

    public static class A {
        @Inject
        @Named("duration")
        public Duration duration;
    }

    private Injector injector;
    @Before
    public void setup() {

        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                Map<String, String> properties = new HashMap<>();
                properties.put("duration", "10S");

                Names.bindProperties(binder(), properties);
                binder().convertToTypes(Matchers.only(TypeLiteral.get(Duration.class)), new DurationConvertor());

            }
        });



    }

    @Test
    public void test() {
        A a = injector.getInstance(A.class);
        assertThat(a.duration).isEqualTo(Duration.ofSeconds(10));

    }


}

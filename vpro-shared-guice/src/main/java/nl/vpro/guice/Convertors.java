package nl.vpro.guice;

import java.time.Duration;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class Convertors extends AbstractModule {
    @Override
    protected void configure() {
        binder().convertToTypes(Matchers.only(TypeLiteral.get(Duration.class)), new DurationConvertor());
    }
}

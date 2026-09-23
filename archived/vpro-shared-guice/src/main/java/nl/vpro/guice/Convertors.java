package nl.vpro.guice;

import com.google.inject.AbstractModule;

/**
 * @author Michiel Meeuwissen
 * @since 1.69
 */
public class Convertors extends AbstractModule {

    public static final Convertors INSTANCE = new Convertors();
    @Override
    protected void configure() {
        DurationConvertor.register(binder());
        TemporalAmountConvertor.register(binder());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + DurationConvertor.class.getSimpleName();
    }
}

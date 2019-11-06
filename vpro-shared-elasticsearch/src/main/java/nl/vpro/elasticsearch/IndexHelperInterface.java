package nl.vpro.elasticsearch;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
public interface IndexHelperInterface<C> {

    void createIndex();

    C client();


}

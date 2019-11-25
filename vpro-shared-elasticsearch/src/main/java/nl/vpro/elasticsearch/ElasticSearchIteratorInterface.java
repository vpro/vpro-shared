package nl.vpro.elasticsearch;

import java.util.Optional;

import nl.vpro.util.CountedIterator;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public interface ElasticSearchIteratorInterface<T> extends CountedIterator<T> {


    Optional<TotalRelation> getSizeQualifier();

    enum TotalRelation {
        EQUAL_TO,
        GREATER_THAN_OR_EQUAL_TO
    }




}

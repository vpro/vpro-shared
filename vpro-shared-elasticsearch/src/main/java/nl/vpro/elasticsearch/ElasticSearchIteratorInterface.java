package nl.vpro.elasticsearch;

import nl.vpro.util.CountedIterator;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public interface ElasticSearchIteratorInterface<T> extends CountedIterator<T>, ElasticSearchIteratorMXBean {


    Optional<TotalRelation> getSizeQualifier();

    enum TotalRelation {
        EQUAL_TO,
        GREATER_THAN_OR_EQUAL_TO
    }
    Instant getStart();
    Long getCount();



    default Optional<Instant> getETA() {
        Long count = getCount();
        if (count != null && count != 0) {
            Instant start = getStart();
            Optional<Long> ts = getTotalSize();
            if (ts.isPresent()) {
                Duration duration = Duration.between(start, Instant.now());
                Duration estimatedTotalDuration = Duration.ofNanos((long) (duration.toNanos() * (ts.get().doubleValue() / getCount())));
                return Optional.of(start.plus(estimatedTotalDuration));
            }
        }
        return Optional.empty();

    }


    // Override for JMX
    @Override
    default Date getStartDate() {
        return Date.from(getStart());
    }
    @Override
    default Date getETADate() {
        return getETA().map(Date::from).orElse(null);
    }
    @Override
    default Long getETASeconds() {
        return getETA().map(i -> (i.toEpochMilli() - getStart().toEpochMilli()) / 1000).orElse(null);
    }

    @Override
    default Long getTotalSizeLong() {
        return getTotalSize().orElse(null);
    }
    @Override
    default double getSpeed() {
        throw new UnsupportedOperationException();
    }

}

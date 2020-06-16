package nl.vpro.jackson2;

/**
 * Several setups at VPRO and at NPO involve a backend system that publishes JSON to ElasticSearch.
 * In some cases this published json must be somewhat adapted, in contrast to when it not yet published.
 * @author Michiel Meeuwissen
 * @since 1.72
 */
public class Views {

    public interface Normal {

    }


    public interface Forward extends Normal {

    }
    public interface Publisher extends Normal {

    }

    /**
     * New fields may be temporary marked 'ForwardPublisher'. Which will mean that {@link Jackson2Mapper#getBackwardsPublisherInstance()} will ignore them.
     *
     * That way we can serialize for checking purposes compatible with old values in ES.
     */
    public interface ForwardPublisher extends Publisher, Forward {

    }

}

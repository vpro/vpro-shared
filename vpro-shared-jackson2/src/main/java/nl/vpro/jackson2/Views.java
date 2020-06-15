package nl.vpro.jackson2;

/**
 * Several setups at VPRO and at NPO involve a backend system that publishes JSON to ElasticSearch.
 * In some cases this published json must be somewhat adapted, in contrast to when it not yet published.
 * @author Michiel Meeuwissen
 * @since 1.72
 */
public class Views {

    public static class Normal {

    }

    public static class Publisher extends Normal {

    }

    /**
     * new field may be temporary marked 'ForwardPublisher'.
     */
    public  static class ForwardPublisher extends Publisher {

    }

}

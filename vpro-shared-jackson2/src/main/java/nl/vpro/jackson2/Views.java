package nl.vpro.jackson2;

import com.google.common.annotations.Beta;

/**
 *  * Several setups at VPRO and at NPO involve a backend system that publishes JSON to ElasticSearch.
 * In some cases this published json must be somewhat adapted, in contrast to when it not yet published.
 * @author Michiel Meeuwissen
 * @since 1.72
 */
public class Views {

    public interface Normal {
    }

    /**
     * Forward compatible view
     */
    public interface Forward extends Normal {
    }

    public interface Publisher extends Normal {
    }

    /**
     * A 'model' related view of the json.
     * <p>
     * This would e.g. imply that some extra fields are present which would otherwise calculable, but it may be useful for the receiving end to
     * receive such a value evaluated.
     * @since 2.33
     */
    @Beta
    public interface Model  {
    }

    /**
     *
     * @since 2.33
     */
    @Beta
    public interface ModelAndNormal extends Model, Normal  {
    }

    /**
     * New fields may be temporary marked 'ForwardPublisher'. Which will mean that {@link Jackson2Mapper#getBackwardsPublisherInstance()} will ignore them.
     * <p>
     * That way we can serialize for checking purposes compatible with old values in ES.
     * <p>
     * So generally this means that a field should be present in the published json, but a full  republication hasn't happened yet
     */
    public interface ForwardPublisher extends Publisher, Forward {
    }

}

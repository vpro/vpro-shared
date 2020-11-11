package nl.vpro.elasticsearchclient;

import org.elasticsearch.client.RestClientBuilder;

/**
 * @author Michiel Meeuwissen
 * @since 2.19
 */
@FunctionalInterface
public interface ESClientBuilderFactory {

    RestClientBuilder getClientBuilder();
}

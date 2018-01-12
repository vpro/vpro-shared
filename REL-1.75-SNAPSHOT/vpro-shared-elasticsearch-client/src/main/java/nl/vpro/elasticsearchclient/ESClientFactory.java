package nl.vpro.elasticsearchclient;


import org.elasticsearch.client.RestClient;

/**
 * @author Michiel Meeuwissen
 * @since 3.6
 */
public interface ESClientFactory {

    /**
     * Returns a client. The client may be cached by the factory.
     */

    RestClient client(String logName);

    default RestClient client(Class clazz) {
        return client(clazz.getName());
    }

}

package nl.vpro.elasticsearch;

import org.elasticsearch.client.Client;

/**
 * @author Michiel Meeuwissen
 * @since 3.6
 */
public interface ESClientFactory {

    /**
     * Returns a client. The client may be cached by the factory.
     */

    Client client(String logName);

    default Client client(Class clazz) {
        return client(clazz.getName());
    }

}

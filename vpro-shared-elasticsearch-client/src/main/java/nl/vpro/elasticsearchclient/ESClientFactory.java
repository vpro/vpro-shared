package nl.vpro.elasticsearchclient;


import org.elasticsearch.client.RestClient;

import java.io.IOException;

/**
 * @author Michiel Meeuwissen
 * @since 3.6
 */
@FunctionalInterface
public interface ESClientFactory extends AutoCloseable {

    /**
     * Returns a client. The client may be cached by the factory.
     */

    RestClient client(String logName);

    default RestClient client(Class<?> clazz) {
        return client(clazz.getName());
    }

    default void close() throws IOException {
        client((String) null).close();
    }

}

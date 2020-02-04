package nl.vpro.elasticsearchclient;


import java.io.IOException;
import java.util.function.Supplier;

import org.elasticsearch.client.RestClient;

/**
 * @author Michiel Meeuwissen
 * @since 3.6
 */
@FunctionalInterface
public interface ESClientFactory extends AutoCloseable, Supplier<RestClient> {

    /**
     * Returns a client. The client may be cached by the factory.
     */

    RestClient client(String logName);

    @Override
    default RestClient get() {
        return client((String) null);
    }

    default RestClient client(Class<?> clazz) {
        return client(clazz.getName());
    }

    default void close() throws IOException {
        get().close();
    }

}

package nl.vpro.elasticsearchclient;


import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

/**
 * @author Michiel Meeuwissen
 * @since 3.6
 */
@FunctionalInterface
public interface ESClientFactory extends AutoCloseable, Supplier<RestClient> {


    default void setHosts(HttpHost... hosts) {
        throw new UnsupportedOperationException();
    }
    /**
     * Returns a client. The client may be cached by the factory.
     */

    RestClient client(String logName, Consumer<RestClient> callback);

    @Override
    default RestClient get() {
        return client(null, (rc) -> {});
    }

    default RestClient client(Class<?> clazz) {
        return client(clazz.getName(), (rc) -> {});
    }

    @Override
    default void close() throws IOException {
        get().close();
    }

    default String invalidate() {
        return "";
    }

    default String logString() {
        return toString();
    }

}

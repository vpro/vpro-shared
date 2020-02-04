package nl.vpro.elasticsearchclient;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.elasticsearch.client.RestClient;

/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class SimpleESClientFactory implements ESClientFactory {
    private final RestClient client;
    private final Supplier<String> string;

    public SimpleESClientFactory(RestClient client, Supplier<String> string) {
        this.client = client;
        this.string = string;
    }

    public SimpleESClientFactory(RestClient client) {
        this(client, () -> "simple");
    }

    @Override
    public RestClient client(String logName, Consumer<RestClient> callback) {
        return client;
    }

    @Override
    public String toString() {
        return string.get();
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}

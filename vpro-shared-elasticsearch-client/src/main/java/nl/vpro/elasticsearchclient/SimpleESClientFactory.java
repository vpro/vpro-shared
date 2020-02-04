package nl.vpro.elasticsearchclient;

import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.function.Supplier;

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
    public RestClient client(String logName) {
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

package nl.vpro.elasticsearchclient;

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

    @Override
    public RestClient client(String logName) {
        return client;
    }

    @Override
    public String toString() {
        return string.get();
    }
}

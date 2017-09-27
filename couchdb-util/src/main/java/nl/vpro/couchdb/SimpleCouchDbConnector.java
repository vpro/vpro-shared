package nl.vpro.couchdb;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import nl.vpro.util.WrappedInputStream;

/**
 * Couchdb support with ektorp (which doesn't work with newer jackson)
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@AllArgsConstructor
@lombok.Builder(builderClassName = "Builder")
public class SimpleCouchDbConnector {


    private final String host;
    private final int port;

    private final String path;

    private final Duration socketTimeout;
    private final Duration connectionTimeout;

    public static class Builder {
        int port = 80;
    }


    public InputStream get(String id) throws IOException {
        return getInputStream("/" + id);
    }


    private InputStream getAll() throws IOException {
        return getInputStream("_all_docs?include_docs=true");
    }

    private InputStream getInputStream(String query) throws IOException {
        return getInputStream(getHttpResponse(query));
    }


    private InputStream getInputStream(CloseableHttpResponse httpResponse) throws IOException {
        final InputStream wrapped = httpResponse.getEntity().getContent();
        return new WrappedInputStream(wrapped) {
            @Override
            public void close() throws IOException {
                super.close();
                httpResponse.close();
            }
        };
    }

    private CloseableHttpResponse getHttpResponse(String query) throws IOException {
        RequestConfig config = RequestConfig.custom()
            .setSocketTimeout((int) socketTimeout.toMillis())
            .setConnectTimeout((int) connectionTimeout.toMillis())
            .build();
        final CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();
        HttpUriRequest uriRequest = new HttpGet("/" + path + query);
        HttpHost httpHost = new HttpHost(host, port);
        final CloseableHttpResponse httpResponse = client.execute(httpHost, uriRequest);
        return httpResponse;
    }
}

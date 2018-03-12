package nl.vpro.couchdb;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import nl.vpro.util.WrappedInputStream;

/**
 * Couchdb support without ektorp (which doesn't work with newer jackson)
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
public class SimpleCouchDbConnector {


    private final String host;
    private final int port;

    private final String path;

    private final Duration socketTimeout;
    private final Duration connectionTimeout;


    @lombok.Builder(builderClassName = "Builder")
    private SimpleCouchDbConnector(

        String host, int port, String path, Duration socketTimeout, Duration connectionTimeout) {
        this.host = host;

        this.port = port <= 0 ? 80 : port;
        this.path = StringUtils.isNotBlank(path) && ! path.endsWith("/") ? path + "/" : path;
        this.socketTimeout = socketTimeout;
        this.connectionTimeout = connectionTimeout;
    }


    public InputStream get(String id) throws IOException {
        return getInputStream(id);
    }


    public InputStream getAll() throws IOException {
        return getInputStream("_all_docs?include_docs=true");
    }


    public InputStream getChanges(String since) throws IOException {
        return getInputStream("_changes?include_docs=true&since=" + since);
    }

    public InputStream getChanges() throws IOException {
        return getInputStream("_changes?include_docs=true");
    }

    public CouchdbChangesIterator getChangesIterator() throws IOException {
        return new CouchdbChangesIterator(getChanges());
    }

    public CouchdbChangesIterator getChangesIterator(String since) throws IOException {
        return new CouchdbChangesIterator(getChanges(since));
    }

    public CouchdbViewIterator getAllIterator() throws IOException {
        return new CouchdbViewIterator(getChanges());
    }

    public long getDocCount() throws IOException {
        return CouchdbStreamIterator.mapper.readTree(getInputStream("")).get("doc_count").longValue();
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
        RequestConfig.Builder config = RequestConfig.custom();

        if (socketTimeout != null) {
            config.setSocketTimeout((int) socketTimeout.toMillis());
        }
        if (connectionTimeout != null) {
            config.setConnectTimeout((int) connectionTimeout.toMillis());
        }

        final CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config.build()).build();

        HttpUriRequest uriRequest = new HttpGet("/" + path + query);

        HttpHost httpHost = new HttpHost(host, port);
        log.debug("Opening {}{}", httpHost, uriRequest);
        final CloseableHttpResponse httpResponse = client.execute(httpHost, uriRequest);
        return httpResponse;
    }

    @Override
    public String toString() {
        return host + ":" + port + "/" + path;
    }
}

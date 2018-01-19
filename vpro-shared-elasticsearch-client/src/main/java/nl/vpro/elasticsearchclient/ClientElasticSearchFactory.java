package nl.vpro.elasticsearchclient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.util.TimeUtils;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
@Data
public class ClientElasticSearchFactory implements AsyncESClientFactory {

    private String clusterName;

    private String unicastHosts;

    private boolean implicitJavaToHttpPort = true;

    private Duration socketTimeout = Duration.ofSeconds(60);
    private Duration connectionTimeout = Duration.ofSeconds(5);
    private Duration maxRetryTimeout = Duration.ofSeconds(60);

    Map<String, RestClient> clients = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("Found {}", this);
    }

    @Override
    public CompletableFuture<RestClient> clientAsync(String logName, Consumer<RestClient> callback) {
        RestClient present = clients.get(logName);
        if (present != null) {
            callback.accept(present);
            return CompletableFuture.completedFuture(present);
        }

        Logger l = LoggerFactory.getLogger(logName);
        HttpHost[] hosts = getHosts();

        final RestClientBuilder clientBuilder = RestClient.builder(hosts);
        final RestClient client = clientBuilder
            .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout((int) connectionTimeout.toMillis())
                .setSocketTimeout((int) socketTimeout.toMillis()))
            .setMaxRetryTimeoutMillis((int) maxRetryTimeout.toMillis())
            .build();

        CompletableFuture<RestClient> future = new CompletableFuture<>();
        IndexHelper helper = IndexHelper.builder()
            .client(new SimpleESClientFactory(client, this::toString))
            .log(l)
            .build();
        helper.getClusterNameAsync().whenComplete((foundClusterName, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            }
            if (clusterName != null && !clusterName.equals(foundClusterName)) {
                future.completeExceptionally(new IllegalStateException(Arrays.toString(hosts) + ": Connected to wrong cluster ('" + foundClusterName + "' != '" + clusterName + "')"));
                return;
            }
            future.complete(client);
            callback.accept(client);
            clients.put(logName, client);
            if (l.isInfoEnabled()) {
                l.info("Connected to cluster '{}'", foundClusterName);
            }
        });
        return future;

    }

    protected HttpHost[] getHosts() {
        return Arrays.stream(unicastHosts.split(("\\s*,\\s*")))
            .map(HttpHost::create)
            .map(h ->
                h.getPort() >= 9300 && implicitJavaToHttpPort ?
                    new HttpHost(h.getHostName(), h.getPort() - 100)
                    : h
            )
            .map(h ->
                h.getPort() == -1 ? new HttpHost(h.getHostName(), 9200) : h
            )
            .toArray(HttpHost[]::new);
    }

    @Override
    public String toString() {
        HttpHost[] hosts = getHosts();
        if (hosts.length > 0) {
            return hosts[0].toString();
        } else {
            return unicastHosts;
        }

    }

    public void setSocketTimeoutDuration(String socketTimeout) {
        this.socketTimeout = TimeUtils.parseDuration(socketTimeout).orElse(this.socketTimeout);
    }

    public void setConnectionTimeoutDuration(String connectionTimeout) {
        this.connectionTimeout = TimeUtils.parseDuration(connectionTimeout).orElse(this.connectionTimeout);
    }

    public void setMaxRetryTimeoutDuration(String maxRetryTimeout) {
        this.maxRetryTimeout = TimeUtils.parseDuration(maxRetryTimeout).orElse(this.maxRetryTimeout);
    }
}

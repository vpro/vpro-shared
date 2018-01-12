package nl.vpro.elasticsearchclient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.Futures;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Data
@Slf4j
public class ClientElasticSearchFactory implements AsyncESClientFactory {

    private String clusterName;

    private String unicastHosts;

    private IndexHelper helper;

    private boolean implicitJavaToHttpPort = true;

    Map<String, RestClient> clients = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("Found {}", this);
    }

    @Override
    public Future<RestClient> clientAsync(String logName, Consumer<RestClient> callback) {
        RestClient present = clients.get(logName);
        if (present != null) {
            callback.accept(present);
            return Futures.immediateFuture(present);
        }

        Logger l = LoggerFactory.getLogger(logName);
        HttpHost[] hosts = Arrays.stream(unicastHosts.split(("\\s*,\\s*")))
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

        final RestClientBuilder clientBuilder = RestClient.builder(hosts);
        final RestClient client = clientBuilder.build();

        CompletableFuture<RestClient> future = new CompletableFuture<>();
        helper = IndexHelper.builder()
            .client((e) -> client)
            .log(l)
            .build();
        helper.getClusterNameAsync((foundClusterName) -> {
            if (clusterName != null && !clusterName.equals(foundClusterName)) {
                throw new IllegalStateException(Arrays.toString(hosts) + ": Connected to wrong cluster ('" + foundClusterName + "' != '" + clusterName + "')");
            }
            future.complete(client);
            callback.accept(client);
            clients.put(logName, client);
            if (l.isInfoEnabled()) {
                l.info("Connected to cluster '{}'", foundClusterName);
                helper.countAsync((count) ->
                    l.info("Cluster '{}' has  {} objects", foundClusterName, count));
            }
        });
        return future;
    }
}

package nl.vpro.elasticsearchclient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Data
@Slf4j
public class ClientElasticSearchFactory implements ESClientFactory {

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
    public RestClient client(String logName) {
        return clients.computeIfAbsent(logName, (ln) -> {
            Logger l = LoggerFactory.getLogger(ln);
            HttpHost[] hosts = Arrays.stream(unicastHosts.split(("\\s*,\\s*")))
                .map(HttpHost::create)
                .map(h ->
                    h.getPort() >= 9300 && implicitJavaToHttpPort
                    ? new HttpHost(h.getHostName(), h.getPort() - 100)
                    : h
                )
                .toArray(HttpHost[]::new);

            RestClientBuilder clientBuilder = RestClient.builder(hosts);
            RestClient client = clientBuilder.build();

            helper = IndexHelper.builder()
                .client((e) -> client)
                .log(l)
                .build();
            String foundClusterName = helper.getClusterName();
            if (clusterName != null && !clusterName.equals(foundClusterName)) {
                throw new IllegalStateException("Connected to wrong cluster " + foundClusterName + " (!=" + clusterName + ")");
            }
            l.info("Connected to {} with {} objects", foundClusterName, helper.count());
            return client;
        });
    }
}

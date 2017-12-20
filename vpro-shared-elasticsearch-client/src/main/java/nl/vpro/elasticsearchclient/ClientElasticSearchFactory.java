package nl.vpro.elasticsearchclient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;


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

    @PostConstruct
    public void init() {
        log.info("Found {}", this);
    }

    @Override
    public RestClient client(String logName) {


        HttpHost[] hosts = Arrays.stream(unicastHosts.split(("\\s*,\\s*")))
            .map(HttpHost::create)
            .map(h -> h.getPort() >=9300 && implicitJavaToHttpPort ? new HttpHost(h.getHostName(), h.getPort() -100) : h)
            .toArray(HttpHost[]::new);

        RestClientBuilder clientBuilder = RestClient.builder(hosts);
        RestClient client = clientBuilder.build();

        helper = IndexHelper.builder()
            .client((e) -> client)
            .build();
        if (clusterName != null && ! helper.getClusterName().equals(clusterName)) {
            throw new IllegalStateException("Connected to wrong cluster");
        }
        return client;
    }
}

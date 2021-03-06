package nl.vpro.elasticsearch7;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.vpro.util.UrlProvider;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class TransportClientFactory implements  ESClientFactory {

    @Getter
    private List<UrlProvider> transportAddresses = Collections.emptyList();

    @Getter
    private String clusterName;

    @Getter
    private boolean implicitHttpToJavaPort = false;

    @Getter
    private boolean ignoreClusterName = false;

    private Client client = null;

    Client buildClient() {
        return client("test");
    }

    @Override
    public Client client(String logName){
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        client = constructClient(logName);
                        log.info("Constructed client {} ({} {}) (on behalf of {}", client, transportAddresses, clusterName, logName);

                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    log.debug("Construction client on behalf of {} not needed (already happend in other thread)", logName);
                }
            }
        }
        return client;


    }

    private Client constructClient(String logName) throws UnknownHostException {
        Settings.Builder builder = Settings
            .builder();

        if (ignoreClusterName || StringUtils.isBlank(clusterName)) {
            builder.put("client.transport.ignore_cluster_name", true);
        }


        if (StringUtils.isNotBlank(clusterName)) {
            builder.put("cluster.name", clusterName);
        }
        builder.put("client.transport.ping_timeout", "15s");
        builder.put("client.transport.nodes_sampler_interval", "15s");
        TransportClient transportClient = new PreBuiltTransportClient(builder.build());

        for (UrlProvider urlProvider : transportAddresses) {
            int port = urlProvider.getPort();
            if (implicitHttpToJavaPort && port < 9300 && port >= 9200) {
                log.info("Port is configured {}, but we need a java protocol port. Taking {}", port, port + 100);
                port += 100;
            }
            transportClient.addTransportAddress(new TransportAddress(InetAddress.getByName(urlProvider.getHost()), port));
        }
        log.debug("Build es client {} {} ({})", logName, transportAddresses, clusterName);

        return transportClient;
    }

    public void setElasticSearchHosts(String string ) {

        int index = string.lastIndexOf(":");
        final String hosts;
        final int defaultPort;
        if (index > 0) {
            hosts = string.substring(0, index);
            defaultPort = Integer.parseInt(string.substring(index + 1, string.length()));
        } else {
            hosts = string;
            defaultPort = 9300;
        }


        this.transportAddresses = Arrays.stream(hosts.split("\\s*,\\s*"))
            .map(s -> {
                String[] split = s.split(":", 2);
                return new UrlProvider(split[0], split.length < 2 ? -1: Integer.parseInt(split[1]));
            }).collect(Collectors.toList());

        for (UrlProvider u : this.transportAddresses) {
            if (u.getPort() == -1) {
                u.setPort(defaultPort);
            }
        }
        reset();

    }


    public void setTransportAddresses(UrlProvider... transportAddresses) {
        this.transportAddresses = Arrays.asList(transportAddresses);
        reset();
    }


    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
        reset();
    }

    public void setImplicitHttpToJavaPort(boolean implicitHttpToJavaPort) {

        this.implicitHttpToJavaPort = implicitHttpToJavaPort;
        reset();
    }

    public void setIgnoreClusterName(boolean ignoreClusterName) {
        reset();
        this.ignoreClusterName = ignoreClusterName;
    }


    private void reset() {
        shutdown();
        client = null;
    }
    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }
    @Override
    public String toString() {
        return transportAddresses + " " + clusterName;
    }
}

package nl.vpro.elasticsearch;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import nl.vpro.util.UrlProvider;

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
                    log.info("Constructing client on behalf of {}", logName);
                    try {
                        client = constructClient(logName);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    log.info("Construction client on behalf of {} not needed (already happend in other thread)", logName);
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
        TransportClient transportClient = new PreBuiltTransportClient(builder.build());

        for (UrlProvider urlProvider : transportAddresses) {
            int port = urlProvider.getPort();
            if (implicitHttpToJavaPort && port < 9300 && port >= 9200) {
                log.info("Port is configured {}, but we need a java protocol port. Taking {}", port, port + 100);
                port += 100;
            }
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(urlProvider.getHost()), port));
        }
        log.info("Build es client {} {} ({})", logName, transportAddresses, clusterName);

        return transportClient;
    }

    public void setElasticSearchHosts(String string ) {

        this.transportAddresses = Arrays.stream(string.split("\\s*,\\s*"))
            .map(s -> {
                String[] split = s.split(":", 2);
                return new UrlProvider(split[0], split.length < 2 ? -1: Integer.parseInt(split[1]));
            }).collect(Collectors.toList());
        int port = -1;
        for (UrlProvider u : this.transportAddresses) {
            if (u.getPort() > -1) {
                port = u.getPort();
            }
        }
        if (port == -1) {
            port = 9300;
        }
        for (UrlProvider u : this.transportAddresses) {
            if (u.getPort() == -1) {
                u.setPort(port);
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

package nl.vpro.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import nl.vpro.util.UrlProvider;

/**
 * @author ernst
 */
@Slf4j
public class ESClientFactoryImpl implements  ESClientFactory {

    private List<UrlProvider> transportAddresses = Collections.emptyList();


    private String clusterName;

    /**
     * When set to true, other cluster nodes are detected automatically
     */
    private boolean sniffCluster = true;

    /**
     * Set to true to ignore cluster name validation of connected nodes
     */
    private boolean ignoreClusterName = false;

    /**
     * How often to sample / ping the nodes listed and connected
     */
    private Integer pingTimeoutInSeconds;

    /**
     * How often should the client check the given node?
     */
    private Integer pingIntervalInSeconds;

    private boolean implicitHttpToJavaPort = false;

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
        Settings.Builder builder = Settings.builder()
            .put("client.transport.ignore_cluster_name", ignoreClusterName)
            .put("client.transport.sniff", sniffCluster)
            .put("discovery.zen.ping.multicast.enabled", "false");

        if (StringUtils.isNotBlank(clusterName)) {
            builder.put("cluster.name", clusterName);
        }
        if (pingTimeoutInSeconds != null) {
            builder.put("client.transport.ping_timeout", pingTimeoutInSeconds + "s");
        }
        if (pingIntervalInSeconds != null) {
            builder.put("client.transport.nodes_sampler_interval", pingIntervalInSeconds + "s");
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



    public void setTransportAddresses(List<UrlProvider> transportAddresses) {
        reset();
        this.transportAddresses = transportAddresses;
    }


    public void setClusterName(String clusterName) {
        reset();
        this.clusterName = clusterName;
    }



    public void setSniffCluster(boolean sniffCluster) {
        reset();
        this.sniffCluster = sniffCluster;
    }


    public void setIgnoreClusterName(boolean ignoreClusterName) {
        reset();
        this.ignoreClusterName = ignoreClusterName;
    }


    public void setPingTimeoutInSeconds(Integer pingTimeoutInSeconds) {
        reset();
        this.pingTimeoutInSeconds = pingTimeoutInSeconds;
    }



    public void setPingIntervalInSeconds(Integer pingIntervalInSeconds) {
        reset();
        this.pingIntervalInSeconds = pingIntervalInSeconds;
    }

    public void setImplicitHttpToJavaPort(boolean implicitHttpToJavaPort) {
        this.implicitHttpToJavaPort = implicitHttpToJavaPort;
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

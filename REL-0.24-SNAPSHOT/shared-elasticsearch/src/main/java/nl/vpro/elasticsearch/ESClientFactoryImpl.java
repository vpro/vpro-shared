package nl.vpro.elasticsearch;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.util.UrlProvider;

/**
 * @author ernst
 */
public class ESClientFactoryImpl implements  ESClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ESClientFactoryImpl.class);

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


    Client buildClient() {
        return buildClient("test");
    }

    @Override
    public Client buildClient(String logName){
        ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder()
            .put("client.transport.ignore_cluster_name", ignoreClusterName)
            .put("client.transport.sniff", sniffCluster)
            .put("discovery.zen.ping.multicast.enabled", "false");

        if(StringUtils.isNotBlank(clusterName)) {
            builder.put("cluster.name", clusterName);
        }
        if(pingTimeoutInSeconds != null) {
            builder.put("client.transport.ping_timeout", pingTimeoutInSeconds + "s");
        }
        if(pingIntervalInSeconds!= null) {
            builder.put("client.transport.nodes_sampler_interval", pingIntervalInSeconds +  "s");
        }

        TransportClient transportClient = new TransportClient(builder.build());
        for (UrlProvider urlProvider : transportAddresses) {
            transportClient.addTransportAddress(new InetSocketTransportAddress(urlProvider.getHost(), urlProvider.getPort()));
        }
        LOG.info("Build es client {} {} ({})", logName, transportAddresses, clusterName);

        return transportClient;

    }




    public void setTransportAddresses(List<UrlProvider> transportAddresses) {
        this.transportAddresses = transportAddresses;
    }


    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }



    public void setSniffCluster(boolean sniffCluster) {
        this.sniffCluster = sniffCluster;
    }


    public void setIgnoreClusterName(boolean ignoreClusterName) {
        this.ignoreClusterName = ignoreClusterName;
    }


    public void setPingTimeoutInSeconds(Integer pingTimeoutInSeconds) {
        this.pingTimeoutInSeconds = pingTimeoutInSeconds;
    }



    public void setPingIntervalInSeconds(Integer pingIntervalInSeconds) {
        this.pingIntervalInSeconds = pingIntervalInSeconds;
    }

}

package nl.vpro.elasticsearch;

import java.util.concurrent.Callable;

import javax.annotation.PreDestroy;


import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simply join the cluster. That is perhaps the best implementation.
 * @author Michiel Meeuwissen
 * @since 0.23
 */
public class JoinClusterClientFactory implements ESClientFactory {

    private Node node;

    private Client client;

    private String clusterName = "myCluster";

    private String nodeName = null;

    private boolean httpEnabled = false;

    private String unicastHosts = null;

    private String tcpPort = "9350-9400";


    private synchronized Node node(Logger logger) {
        if (node == null) {

            Settings settings = getSettings(logger);
            logger.info("Creating es node for {}", this);
            node = NodeBuilder.nodeBuilder()

                .clusterName(clusterName)
                .client(true)
                .local(false)
                .settings(settings)
                .node();

        }

        return node;
    }

    private Settings getSettings(Logger logger) {
        if (logger == null) {
            logger = LoggerFactory.getLogger(JoinClusterClientFactory.class);
        }
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder()
            .put("http.enabled", httpEnabled);

        if (StringUtils.isNotEmpty(unicastHosts)) {
            settings.put("discovery.zen.ping.multicast.enabled", false);
            settings.put("discovery.zen.ping.unicast.enabled", true);
            settings.put("discovery.zen.ping.unicast.hosts", unicastHosts);
        } else {
            logger.warn("No unicast hosts set. Will use multicast");
            settings.put("discovery.zen.ping.multicast.enabled", true);
            settings.put("discovery.zen.ping.unicast.enabled", false);
        }
        if (nodeName != null) {
            settings.put("node.name", nodeName);
        } else {
            logger.info("node name not set");
        }
        settings.put("transport.tcp.port", tcpPort);
        return settings.build();
    }

    public Callable<Client> client(Logger logger) {
        return () -> {
            if (client != null) {
                return client;
            }
            synchronized (JoinClusterClientFactory.this) {
                if (client == null) {
                    Node n = node(logger);
                    client = n.client();
                }
            }
            return client;

        };
    }

    private void reset() {
        shutdown();
        node = null;
        client = null;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        reset();
        this.clusterName = clusterName;
    }


    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public void setHttpEnabled(boolean httpEnabled) {
        reset();
        this.httpEnabled = httpEnabled;
    }

    public String getUnicastHosts() {
        return unicastHosts;
    }

    public void setUnicastHosts(String unicastHosts) {
        reset();
        this.unicastHosts = unicastHosts;
    }

    public String getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(String tcpPort) {
        reset();
        this.tcpPort = tcpPort;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        reset();
        this.nodeName = nodeName;
    }

    @Override
    public Client client(String logName) {
        try {
            return client(LoggerFactory.getLogger(logName)).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (node != null) {
            node.close();
        }
    }

    @Override
    public String toString() {

        return "ES " + (nodeName == null ? "" : (nodeName + "@")) + clusterName + (StringUtils.isNotBlank(unicastHosts) ? (" (" + unicastHosts + ")") : "") + getSettings(null).getAsMap();
    }


}

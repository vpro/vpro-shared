package nl.vpro.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

/**
 * Simply join the cluster. That is perhaps the best implementation. Though ES made it deprecated.
 * @author Michiel Meeuwissen
 * @since 0.23
 */
@Slf4j
@Deprecated
public class JoinClusterClientFactory implements ESClientFactory {

    private Node node;

    private Client client;

    private String clusterName = "myCluster";

    private String nodeName = JoinClusterClientFactory.class.getSimpleName();

    private boolean httpEnabled = false;

    private String unicastHosts = null;

    private int defaultPort = 9300;

    private String tcpPort = "9350-9400";

    private String networkHost = null;

    private String pathHome = "/tmp/";

    private Map<String, String> additionalSettings = new HashMap<>();

    private Settings getSettings(Logger logger) {
        if (logger == null) {
            logger = NOPLogger.NOP_LOGGER;
        }
        Settings.Builder settings = Settings.builder();

        if (httpEnabled) {
            settings
                .put("http.enabled", httpEnabled)
                .put("transport.profiles.default.port", defaultPort)
            ;
        }


        if (StringUtils.isNotEmpty(unicastHosts)) {
            settings.put("discovery.zen.ping.unicast.hosts", unicastHosts);
        } else {
            logger.warn("No unicast hosts set. Will use multicast");
        }
        if (networkHost != null) {
            settings.put("network.host", networkHost);
        }
        if (StringUtils.isBlank(tcpPort)) {
            tcpPort = "9350-9400";
        }
        settings
            .put("transport.tcp.port", tcpPort)
            .put(Node.NODE_NAME_SETTING.getKey(), nodeName)
            .put(additionalSettings)
            .put(Node.NODE_MASTER_SETTING.getKey(), false)
            .put(Node.NODE_DATA_SETTING.getKey(), false)
            .put("transport.type", "netty4")
            .put("http.type", "netty4")
            .put("path.home", pathHome)
            .put("cluster.name", clusterName)
        ;
        ;
        return settings.build();
    }

    public Callable<Client> client(Logger logger) {
        return () -> {
            if (client != null) {
                return client;
            }
            synchronized (JoinClusterClientFactory.this) {
                if (client == null) {
                    Settings settings = getSettings(logger);
                    Collection<Class<? extends Plugin>> plugins = Collections.singletonList(Netty4Plugin.class);
                    node = new PluginConfigurableNode(settings, plugins).start();

                    node.start();
                    client = node.client();
                }
            }
            return client;

        };
    }

    private void reset()  {
        shutdown();
        client = null;
        node = null;
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

    public String getPathHome() {
        return pathHome;
    }

    public void setPathHome(String pathHome) {
        reset();
        this.pathHome = pathHome;
    }

    public String getUnicastHosts() {
        return unicastHosts;
    }

    /**
     * The hosts to connect to via unicast. If empty, then multicast is used.
     */
    public void setUnicastHosts(String unicastHosts) {
        this.unicastHosts = unicastHosts;
        reset();
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
        reset();
    }

    public String getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(String tcpPort) {
        this.tcpPort = tcpPort;
        reset();
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
        reset();
    }

    public String getNetworkHost() {
        return networkHost;
    }

    public void setNetworkHost(String networkName) {
        this.networkHost = networkName;
        reset();
    }

    public Map<String, String> getAdditionalSettings() {
        return additionalSettings;
    }

    public void setAdditionalSettings(Map<String, String> additionalSettings) {
        this.additionalSettings = additionalSettings;
        reset();
    }

    @Override
    public Client client(String logName) {
        try {
            return client(LoggerFactory.getLogger(logName)).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        log.info("Created {}", this);

    }


    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.close();
        }
        if (node != null){
            try {
                node.close();
            } catch(IOException ioe) {
                log.warn(ioe.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        return "ES " + (nodeName == null ? "" : (nodeName + "@")) + clusterName + (StringUtils.isNotBlank(unicastHosts) ? (" (" + unicastHosts + ")") : "") + getSettings(null).getAsMap();
    }


    private static class PluginConfigurableNode extends Node {
        public PluginConfigurableNode(Settings settings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(InternalSettingsPreparer.prepareEnvironment(settings, null), classpathPlugins);
        }
    }

}

/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.elasticsearch;

import java.util.concurrent.Callable;

import javax.annotation.PreDestroy;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class EmbeddedClientFactory implements ESClientFactory {

    private Node node;

    private Client client;

    private String path = "/tmp";

    private String clusterName = "myCluster";

    private int numberOfMasters = 2;


    public synchronized Node node() {
        if(node == null) {
            Settings settings = ImmutableSettings.settingsBuilder()
                .put("http.enabled", true)
                .put("index.store.type", "niofs")
                .put("client.transport.sniff", true)
                .put("node.data", true)
                .put("node.master", true)
                .put("cluster.name", clusterName)
                .put("index.number_of_shards", 2)
                .put("index.number_of_replicas", 1)
                .put("discovery.zen.minimum_master_nodes", numberOfMasters)
                .put("discovery.zen.ping_timeout", 5)
                .put("discovery.zen.ping.multicast.enabled", true)
                .put("path.home", path).build();

            node = NodeBuilder.nodeBuilder()
                .local(false)
                .settings(settings)
                .node();

        }

        return node;
    }

    public Callable<Client> client(Logger logger) {
        return () -> {
            if (client != null) {
                return client;
            }
            synchronized(EmbeddedClientFactory.this) {
                if (client == null) {
                    logger.info("Creating client");
                    client = node().client();
                    logger.info("Waiting for green");
                    client.admin().cluster().prepareHealth().setWaitForGreenStatus().get();

                }
            }
            return client;

        };
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        node = null;
        client = null;
        this.path = path;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        node = null;
        client = null;
        this.clusterName = clusterName;
    }

    public int getNumberOfMasters() {
        return numberOfMasters;
    }

    public void setNumberOfMasters(int numberOfMasters) {
        this.numberOfMasters = numberOfMasters;
    }

    @Override
    public Client buildClient(String logName) {
        try {
            return client(LoggerFactory.getLogger(logName)).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.admin().cluster().prepareHealth().setWaitForGreenStatus().get();
            client.close();
        }
        if (node != null) {
            node.close();
        }
    }

    @Override
    public String toString() {
        return "ES " + clusterName + "@" + path;
    }
}

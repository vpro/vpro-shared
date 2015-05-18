/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.elasticsearch;

import javax.annotation.PreDestroy;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * @author Michiel Meeuwissen
 * @since 0.21
 */
public class EmbeddedClientFactory implements ESClientFactory {

    private Node node;

    private Client client;

    private String path = "/tmp";

    private String clusterName = "myCluster";


    public synchronized Node node() {
        if(node == null) {
            Settings settings = ImmutableSettings.settingsBuilder()
                .put("http.enabled", true)
                .put("index.store.type", "niofs")
                .put("client.transport.sniff", true)
                .put("node.data", true)
                .put("node.master", true)
                .put("cluster.name", clusterName)
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put("discovery.zen.minimum_master_nodes", 1)
                .put("discovery.zen.ping_timeout", 5)
                .put("path.home", path).build();

            node = NodeBuilder.nodeBuilder()
                .local(false)
                .settings(settings)
                .node();

        }

        return node;
    }

    public synchronized Client client() {
        if(client == null) {
            client = node().client();
            client.admin().cluster().prepareHealth().setWaitForYellowStatus().get();
        }
        return client;
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

    @Override
    public Client buildClient(String logName) {
        return client();

    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
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

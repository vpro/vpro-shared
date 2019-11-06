/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.elasticsearch;

import lombok.ToString;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * @author Roelof Jan Koekoek
 * @since 3.6
 */
@ToString
public class LocalClientFactory implements ESClientFactory {

    private Node node;

    private Client client;

    private String path = null;

    public synchronized Node node() {
        if(node == null) {
            ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder()
                .put("http.enabled", "true")
                .put("gateway.type", "none")
                .put("index.store.type", "memory");

            if (path != null) {
                builder
                    .put("index.store.type", "simplefs")
                    .put("path.home", path).build();
            }

            node = NodeBuilder.nodeBuilder()
                .local(true)
                .settings(builder)
                .node();
        }

        return node;
    }

    public synchronized Client client() {
        if(client == null) {
            client = node().client();
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

    @Override
    public Client client(String logName) {
        return client();

    }
}

/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.elasticsearch;

import lombok.ToString;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * @author Roelof Jan Koekoek
 * @since 3.6
 */
@ToString
public class LocalClientFactory implements ESClientFactory {


    private Client client;

    private String path = null;


    public synchronized Client client() {
        if(client == null) {
            Settings.Builder builder = Settings.builder()
                .put("http.enabled", "true")
                //.put("gateway.type", "none")
                .put("index.store.type", "memory");

            if (path != null) {
                builder
                    .put("index.store.type", "simplefs")
                    .put("path.home", path).build();
            }

            client = new PreBuiltTransportClient(builder.build());
        }
        return client;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        client = null;
        this.path = path;
    }

    @Override
    public Client client(String logName) {
        return client();

    }

    public void shutdown() {
        if (client != null) {

        }

    }
}

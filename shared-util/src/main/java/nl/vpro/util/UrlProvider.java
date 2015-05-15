/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

/**
 * User: rico
 * Date: 05/04/2012
 */
public class UrlProvider {
    private final String host;
    private final int port;
    private String uri;


    public UrlProvider(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUrl() {
        StringBuilder buffer = new StringBuilder("http://");
        buffer.append(host);
        if (port > 0 && port != 80) {
            buffer.append(":").append(port);
        }

        buffer.append("/");
        if (uri != null) {
            buffer.append(uri);
        }
        return buffer.toString();
    }

    @Override
    public String toString() {
        return getUrl();
    }
}

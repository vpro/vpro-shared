/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.util;

import java.net.URI;

/**
 * @author rico
 */
public class UrlProvider {

    private String scheme = "http";
    private String host;
    private int port;
    private String path;


    public static UrlProvider fromUrl(String uri) {
        UrlProvider provider = new UrlProvider();
        provider.setUrl(uri);
        return provider;
    }

    public UrlProvider() {

    }

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

    public String getPath() {
        return path;
    }

    @Deprecated
    public String getUri() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public void setUrl(String url) {
        URI uri = URI.create(url);
        this.host = uri.getHost();
        this.port = uri.getPort();
        if (this.port == -1) {
            this.port = 80;
        }
        this.path = uri.getPath();
        if (this.path != null && this.path.startsWith("/")) {
            this.path = this.path.substring(1);
        }
        this.scheme = uri.getScheme();
        if (scheme == null) {
            switch(port) {
                case 80:
                    scheme = "http";
                    break;
                case 443:
                    scheme = "https";
            }
        }
    }

    public String getUrl() {
        StringBuilder buffer = new StringBuilder(scheme + "://");
        buffer.append(host);
        if (port > 0 && (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443))) {
            buffer.append(":").append(port);
        }

        buffer.append("/");
        if (path != null) {
            buffer.append(path);
        }
        return buffer.toString();
    }

    @Override
    public String toString() {
        return getUrl();
    }
}

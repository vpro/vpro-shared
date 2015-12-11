/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.util.ThreadPools;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class AbstractApiClient {

    static {
        try {
            ResteasyProviderFactory resteasyProviderFactory = ResteasyProviderFactory.getInstance();
            try {
                JacksonContextResolver jacksonContextResolver = new JacksonContextResolver();
                resteasyProviderFactory.registerProviderInstance(jacksonContextResolver);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //resteasyProviderFactory.addClientErrorInterceptor(new NpoApiClientErrorInterceptor());
            //resteasyProviderFactory.addClientExceptionMapper(new ExceptionMapper());


            RegisterBuiltin.register(resteasyProviderFactory);
        } catch (Throwable t) {
            System.err.println(t.getMessage());
        }
    }

    protected final ClientHttpEngine clientHttpEngine;
    protected final ClientHttpEngine clientHttpEngineNoTimeout;

    private Thread connectionGuard;

    private boolean shutdown = false;

    public AbstractApiClient(int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        clientHttpEngine = buildHttpEngine(connectionTimeoutMillis, maxConnections, connectionInPoolTTL);
        clientHttpEngineNoTimeout = buildHttpEngine(0/*value of zero is interpreted as infinite timeout*/, 3, -1);
    }

    protected ApacheHttpClient4Engine buildHttpEngine(int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        return new ApacheHttpClient4Engine(getHttpClient43(connectionTimeoutMillis, maxConnections, connectionInPoolTTL));
    }

    // See https://issues.jboss.org/browse/RESTEASY-975
    // You _must_ use httpclient 4.2.1 syntax.  Otherwise timeout settings will simply not work
    // See also https://jira.vpro.nl/browse/MGNL-11312
    // This code can be used when this will be fixed in resteasy.
    private HttpClient getHttpClient43(int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        SocketConfig socketConfig = SocketConfig.custom()
            .setTcpNoDelay(true)
            .setSoKeepAlive(true)
            .setSoReuseAddress(true)
            .build();

        PoolingHttpClientConnectionManager poolingClientConnectionManager = new PoolingHttpClientConnectionManager(connectionInPoolTTL, TimeUnit.MILLISECONDS);
        poolingClientConnectionManager.setDefaultMaxPerRoute(maxConnections);
        poolingClientConnectionManager.setMaxTotal(maxConnections);
        poolingClientConnectionManager.setDefaultSocketConfig(socketConfig);

        if (maxConnections > 1) {
            watchIdleConnections(poolingClientConnectionManager);
        }

        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setExpectContinueEnabled(true)
            .setStaleConnectionCheckEnabled(false)
            .setMaxRedirects(100)
            .setConnectionRequestTimeout(connectionTimeoutMillis)
            .setConnectTimeout(connectionTimeoutMillis)
            .setSocketTimeout(connectionTimeoutMillis)
            .build();

        List<Header> defaultHeaders = new ArrayList<>();
        defaultHeaders.add(new BasicHeader("Keep-Alive", "timeout=1000, max=500"));

        HttpClient client = HttpClients.custom()
            .setConnectionManager(poolingClientConnectionManager)
            .setDefaultRequestConfig(defaultRequestConfig)
            .setDefaultHeaders(defaultHeaders)
            .setKeepAliveStrategy(new MyConnectionKeepAliveStrategy())
            .build();
        return client;
    }

    // should be used as long as resteasy uses http client < 4.3
    private HttpClient getHttpClient(int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {

        PoolingClientConnectionManager poolingClientConnectionManager = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault(),
            connectionInPoolTTL, TimeUnit.MILLISECONDS);
        poolingClientConnectionManager.setDefaultMaxPerRoute(maxConnections);
        poolingClientConnectionManager.setMaxTotal(maxConnections);

        if (maxConnections > 1) {
            watchIdleConnections(poolingClientConnectionManager);
        }

        HttpParams httpParams = new BasicHttpParams();

        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeoutMillis);
        HttpConnectionParams.setSoTimeout(httpParams, connectionTimeoutMillis);

        return new DefaultHttpClient(poolingClientConnectionManager, httpParams);

    }

    public ClientHttpEngine getClientHttpEngine() {
        return clientHttpEngine;
    }

    @PreDestroy
    public void shutdown() {
        if(!shutdown) {
            shutdown = true;
            notifyAll();
            connectionGuard = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    private class MyConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {

            HttpRequestWrapper wrapper = (HttpRequestWrapper)context.getAttribute(HttpClientContext.HTTP_REQUEST);
            if(wrapper.getURI().getPath().endsWith("/media/changes")) {
                // 30 minutes
                return 30 * 60 * 1000;
            }
            // Honor 'keep-alive' header
            HeaderElementIterator it = new BasicHeaderElementIterator(
                response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while(it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if(value != null && param.equalsIgnoreCase("timeout")) {
                    try {
                        // as responded
                        return Long.parseLong(value) * 1000;
                    } catch(NumberFormatException ignore) {
                    }
                }
            }
            // 1 minute
            return 60 * 1000;
        }
    }

   private void watchIdleConnections(final PoolingClientConnectionManager connectionManager) {
        ThreadFactory threadFactory = ThreadPools.createThreadFactory("API Client purge idle connections", false, Thread.NORM_PRIORITY);
        connectionGuard = threadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                while(!shutdown) {
                    try {
                        synchronized(this) {
                            wait(5000);
                            connectionManager.closeExpiredConnections();
                            //connectionManager.closeIdleConnections(connectionTimeoutMillis, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        });
        connectionGuard.start();
    }

    private void watchIdleConnections(final PoolingHttpClientConnectionManager connectionManager) {
        ThreadFactory threadFactory = ThreadPools.createThreadFactory("API Client purge idle connections", false, Thread.NORM_PRIORITY);
        connectionGuard = threadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                while (!shutdown) {
                    try {
                        synchronized (this) {
                            wait(5000);
                            connectionManager.closeExpiredConnections();
                            //connectionManager.closeIdleConnections(connectionTimeoutMillis, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        });
        connectionGuard.start();
    }
}

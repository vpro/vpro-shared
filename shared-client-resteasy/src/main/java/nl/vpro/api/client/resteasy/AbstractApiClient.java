/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.XTrustProvider;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class AbstractApiClient implements  AbstractApiClientMBean {

    private static Logger LOG = LoggerFactory.getLogger(AbstractApiClient.class);

    private static Thread connectionGuardThread;
    private static final ThreadFactory THREAD_FACTORY = ThreadPools.createThreadFactory("API Client purge idle connections", true, Thread.NORM_PRIORITY);
    private static final ConnectionGuard GUARD = new ConnectionGuard();
    static {
        try {
            ResteasyProviderFactory resteasyProviderFactory = ResteasyProviderFactory.getInstance();
            try {


                if (! resteasyProviderFactory.isRegistered(JacksonContextResolver.class)) {
                    JacksonContextResolver jacksonContextResolver = new JacksonContextResolver();
                    resteasyProviderFactory.registerProviderInstance(jacksonContextResolver);
                } else {
                    LOG.info("Already registered {}", JacksonContextResolver.class);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //resteasyProviderFactory.addClientErrorInterceptor(new NpoApiClientErrorInterceptor());
            //resteasyProviderFactory.addClientExceptionMapper(new ExceptionMapper());


            RegisterBuiltin.register(resteasyProviderFactory);
        } catch (Throwable t) {
            LOG.error(t.getClass().getName() + " " + t.getMessage());
        }

    }

    protected ClientHttpEngine clientHttpEngine;
    protected ClientHttpEngine clientHttpEngineNoTimeout;

    private List<PoolingHttpClientConnectionManager> connectionManagers = new ArrayList<>();
    private boolean shutdown = false;
    private boolean trustAll = false;

    Duration connectionRequestTimeout;
    Duration connectTimeout;
    Duration socketTimeout;

    private int maxConnections;
    Duration connectionInPoolTTL;

    public AbstractApiClient(
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        Duration connectionInPoolTTL) {

        this.connectionRequestTimeout = connectionRequestTimeout;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.maxConnections = maxConnections;
        this.connectionInPoolTTL = connectionInPoolTTL;
    }

    public AbstractApiClient(Integer connectionTimeout, int maxConnections, int connectionInPoolTTL) {
        this(Duration.ofMillis(connectionTimeout), Duration.ofMillis(connectionTimeout), Duration.ofMillis(connectionTimeout), maxConnections, Duration.ofMillis(connectionInPoolTTL));
    }


    protected synchronized void invalidate() {
        this.clientHttpEngine = null;
    }

    @Override
    public Duration getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    @Override
    public synchronized void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
        invalidate();
    }

    @Override
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public synchronized void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        invalidate();
    }

    @Override
    public Duration getSocketTimeout() {
        return socketTimeout;
    }

    @Override
    public synchronized void setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
        invalidate();
    }

    public void setTrustAll(boolean b) {
        this.trustAll = b;
        if (trustAll) {
            XTrustProvider.install();
        }
    }



    // See https://issues.jboss.org/browse/RESTEASY-975
    // You _must_ use httpclient 4.2.1 syntax.  Otherwise timeout settings will simply not work
    // See also https://jira.vpro.nl/browse/MGNL-11312
    // This code can be used when this will be fixed in resteasy.
    private  HttpClient getHttpClient43(
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        Duration connectionInPoolTTL) {
        SocketConfig socketConfig = SocketConfig.custom()
            .setTcpNoDelay(true)
            .setSoKeepAlive(true)
            .setSoReuseAddress(true)
            .build();

        PoolingHttpClientConnectionManager connectionManager = null;
        if (connectionInPoolTTL != null) {
            connectionManager = new PoolingHttpClientConnectionManager(connectionInPoolTTL.toMillis(), TimeUnit.MILLISECONDS);
            connectionManager.setDefaultMaxPerRoute(maxConnections);
            connectionManager.setMaxTotal(maxConnections);
            connectionManager.setDefaultSocketConfig(socketConfig);

            if (maxConnections > 1) {
                watchIdleConnections(connectionManager);
            }
        }


        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setExpectContinueEnabled(true)
            .setStaleConnectionCheckEnabled(false)
            .setMaxRedirects(100)
            .setConnectionRequestTimeout(connectionRequestTimeout == null ? 0 : (int) connectionRequestTimeout.toMillis())
            .setConnectTimeout(connectTimeout == null ? 0 : (int) connectTimeout.toMillis())
            .setSocketTimeout(socketTimeout == null ? 0 : (int) socketTimeout.toMillis())
            .build();

        List<Header> defaultHeaders = new ArrayList<>();
        defaultHeaders.add(new BasicHeader("Keep-Alive", "timeout=1000, max=500"));


        HttpClientBuilder client = HttpClients.custom()
            .setDefaultRequestConfig(defaultRequestConfig)
            .setDefaultHeaders(defaultHeaders)
            .setKeepAliveStrategy(new MyConnectionKeepAliveStrategy())
            ;
        if (connectionManager != null){
            client.setConnectionManager(connectionManager);
        }

        if (trustAll){
            try {
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                client.setSSLSocketFactory(sslsf);
            } catch (NoSuchAlgorithmException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return client.build();
    }

    public synchronized ClientHttpEngine getClientHttpEngine() {
        if (clientHttpEngine == null) {
            clientHttpEngine = new ApacheHttpClient4Engine(getHttpClient43(connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, connectionInPoolTTL));
        }
        return clientHttpEngine;
    }

    public synchronized ClientHttpEngine getClientHttpEngineNoTimeout() {
        if (clientHttpEngineNoTimeout == null) {
            clientHttpEngineNoTimeout = new ApacheHttpClient4Engine(getHttpClient43(connectionRequestTimeout, connectTimeout, null, 3, null));
        }
        return clientHttpEngineNoTimeout;

    }


    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        clientHttpEngine = null;
    }

    public Duration getConnectionInPoolTTL() {
        return connectionInPoolTTL;
    }

    public void setConnectionInPoolTTL(Duration connectionInPoolTTL) {
        this.connectionInPoolTTL = connectionInPoolTTL;
        clientHttpEngine = null;
    }

    @PreDestroy
    public void shutdown() {
        if(!shutdown) {
            shutdown = true;
            for (PoolingHttpClientConnectionManager connectionManager : connectionManagers) {
                unwatchIdleConnections(connectionManager);
            }
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

    private synchronized void watchIdleConnections(PoolingHttpClientConnectionManager connectionManager) {
        LOG.debug("Watching idle connections in {}", connectionManager);
        GUARD.connectionManagers.add(connectionManager);
        connectionManagers.add(connectionManager);
        if (connectionGuardThread == null) {
            GUARD.start();
            connectionGuardThread = THREAD_FACTORY.newThread(GUARD);
            connectionGuardThread.start();
        }
    }

    private synchronized void unwatchIdleConnections(PoolingHttpClientConnectionManager connectionManager) {
        LOG.debug("Unwatching idle connections in {}", connectionManager);
        GUARD.connectionManagers.remove(connectionManager);
        if (GUARD.connectionManagers.isEmpty()) {
            connectionGuardThread.interrupt();
            GUARD.shutdown();
            connectionGuardThread = null;
        }
    }

    private static class ConnectionGuard implements Runnable {

        private boolean shutdown = false;
        private List<PoolingHttpClientConnectionManager> connectionManagers = new ArrayList<>();

        void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

        void start() {
            shutdown = false;
        }
        @Override
        public void run() {
            while (!shutdown) {
                try {
                    synchronized (this) {
                        wait(5000);
                        for (PoolingHttpClientConnectionManager connectionManager : connectionManagers) {
                            connectionManager.closeExpiredConnections();
                        }
                        //connectionManager.closeIdleConnections(connectionTimeoutMillis, TimeUnit.MILLISECONDS);
                    }
                } catch (InterruptedException ignored) {
                    LOG.debug(ignored.getMessage());
                }
            }
            LOG.info("Shut down connection guard");
        }
    }
}

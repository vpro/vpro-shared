/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.management.*;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;

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
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.util.LeaveDefaultsProxyHandler;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.TimeUtils;
import nl.vpro.util.XTrustProvider;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Slf4j
public abstract class AbstractApiClient implements AbstractApiClientMXBean {

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
                    log.info("Already registered {}", JacksonContextResolver.class);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //resteasyProviderFactory.addClientErrorInterceptor(new NpoApiClientErrorInterceptor());
            //resteasyProviderFactory.addClientExceptionMapper(new ExceptionMapper());

            RegisterBuiltin.register(resteasyProviderFactory);
        } catch (Throwable t) {
            log.error(t.getClass().getName() + " " + t.getMessage());
        }
    }

    protected final String baseUrl;

    private ClientHttpEngine clientHttpEngine;
    private ClientHttpEngine clientHttpEngineNoTimeout;

    private List<PoolingHttpClientConnectionManager> connectionManagers = new ArrayList<>();
    private boolean shutdown = false;
    private boolean trustAll = false;

    Duration connectionRequestTimeout;
    Duration connectTimeout;
    Duration socketTimeout;

    private int maxConnections;
    private int maxConnectionsPerRoute;
    Duration connectionInPoolTTL;
    protected final Map<Method, Counter> counter = new HashMap<>();
    private Duration countWindow = Duration.ofMillis(15);

    private List<Locale> acceptableLanguages = new ArrayList<>();

    private MediaType mediaType;

    protected AbstractApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        int maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        List<Locale> acceptableLanguages,
        MediaType accept,
        Boolean trustAll
        ) {

        this.connectionRequestTimeout = connectionRequestTimeout;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.maxConnections = maxConnections;
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        this.connectionInPoolTTL = connectionInPoolTTL;
        this.baseUrl = baseUrl == null ? null : (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1): baseUrl);
        this.countWindow = countWindow;
        this.acceptableLanguages = acceptableLanguages;
        this.mediaType = accept;
        if (trustAll != null) {
            setTrustAll(trustAll);
        }
        registerBean();
    }

    protected AbstractApiClient(String baseUrl, Integer connectionTimeout, int maxConnections, int maxConnectionsPerRoute, Integer connectionInPoolTTL) {
        this(baseUrl,
            Duration.ofMillis(connectionTimeout == null ? -1 : connectionTimeout),
            Duration.ofMillis(connectionTimeout == null ? -1 : connectionTimeout),
            Duration.ofMillis(connectionTimeout == null ? -1 : connectionTimeout),
            maxConnections,
            maxConnectionsPerRoute,
            Duration.ofMillis(connectionInPoolTTL == null ? -1 : connectionInPoolTTL),
            Duration.ofMinutes(15),
            null,
            null,
            false
        );
    }

    public synchronized void invalidate() {
        this.clientHttpEngine = null;
    }

    @Override
    public String getConnectionRequestTimeout() {
        return String.valueOf(connectionRequestTimeout);
    }

    @Override
    public synchronized void setConnectionRequestTimeout(String connectionRequestTimeout) {
        this.connectionRequestTimeout = TimeUtils.parseDuration(connectionRequestTimeout).orElse(null);
        invalidate();
    }

    @Override
    public String getConnectTimeout() {
        return String.valueOf(connectTimeout);
    }

    @Override
    public synchronized void setConnectTimeout(String connectTimeout) {
        this.connectTimeout = TimeUtils.parseDuration(connectTimeout).orElse(null);
        invalidate();
    }

    @Override
    public String getSocketTimeout() {
        return String.valueOf(socketTimeout);
    }

    @Override
    public synchronized void setSocketTimeout(String socketTimeout) {
        this.socketTimeout = TimeUtils.parseDuration(socketTimeout).orElse(null);
        invalidate();
    }

    public MediaType getAccept() {
        return mediaType;
    }

    public void setAccept(MediaType mediaType) {
        this.mediaType = mediaType;
        this.invalidate();
    }

    public void setTrustAll(boolean b) {
        this.trustAll = b;
        if (trustAll) {
            XTrustProvider.install();
        }
        invalidate();
    }

    private void registerBean() {
        registerBean(getObjectName(), this);
    }

    protected static void registerBean(ObjectName name, Object object) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            if (mbs.isRegistered(name)) {
                try {
                    mbs.unregisterMBean(name);
                } catch (InstanceNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
            mbs.registerMBean(object, name);
        } catch (NotCompliantMBeanException | MBeanRegistrationException | InstanceAlreadyExistsException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected ObjectName getObjectName() {
        try {
            return new ObjectName("nl.vpro.api.client:type=" + getClass().getSimpleName());

        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClient getHttpClient(
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        int maxConnectionsPerRoute,
        Duration connectionInPoolTTL) {
        SocketConfig socketConfig = SocketConfig.custom()
            .setTcpNoDelay(true)
            .setSoKeepAlive(true)
            .setSoReuseAddress(true)
            .build();

        PoolingHttpClientConnectionManager connectionManager = null;
        if (connectionInPoolTTL != null) {
            connectionManager = new PoolingHttpClientConnectionManager(connectionInPoolTTL.toMillis(), TimeUnit.MILLISECONDS);
            connectionManager.setMaxTotal(maxConnections);
            connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
            connectionManager.setDefaultSocketConfig(socketConfig);

            if (maxConnections > 1) {
                watchIdleConnections(connectionManager);
            }
        }

        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setExpectContinueEnabled(true)
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
            .setKeepAliveStrategy(new MyConnectionKeepAliveStrategy());

        if (connectionManager != null){
            client.setConnectionManager(connectionManager);
        }

        if (trustAll){
            try {
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(), NoopHostnameVerifier.INSTANCE);
                client.setSSLSocketFactory(sslsf);
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
            }
        }

        HttpClient built  = client.build();

        return built;
    }

    public synchronized ClientHttpEngine getClientHttpEngine() {
        if (clientHttpEngine == null) {
            clientHttpEngine = new ApacheHttpClient4Engine(
                getHttpClient(connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL)
            );
        }
        return clientHttpEngine;
    }

    public synchronized ClientHttpEngine getClientHttpEngineNoTimeout() {
        if (clientHttpEngineNoTimeout == null) {
            clientHttpEngineNoTimeout = new ApacheHttpClient4Engine(
                getHttpClient(connectionRequestTimeout, connectTimeout, null, 3, 3, null)
            );
        }
        return clientHttpEngineNoTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        invalidate();
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        invalidate();
    }

    public Duration getConnectionInPoolTTL() {
        return connectionInPoolTTL;
    }

    public void setConnectionInPoolTTL(Duration connectionInPoolTTL) {
        this.connectionInPoolTTL = connectionInPoolTTL;
        clientHttpEngine = null;
        invalidate();
    }

    public Duration getCountWindow() {
        return countWindow;
    }

    public void setCountWindow(Duration countWindow) {
        this.countWindow = countWindow;
        invalidate();
    }

    public List<Locale> getAcceptableLanguages() {
        return acceptableLanguages;
    }

    public void setAcceptableLanguages(List<Locale> acceptableLanguages) {
        this.acceptableLanguages = acceptableLanguages;
        invalidate();
    }

    protected <T, S> T build(ClientHttpEngine engine, Class<T> service, Class<S> restEasyService, Class<?> errorClass) {
        T proxy;
        if (restEasyService == null) {
            proxy = buildResteasy(engine, service);
        } else {
            S resteasy = buildResteasy(engine, restEasyService);
            proxy = (T) Proxy.newProxyInstance(
                AbstractApiClient.class.getClassLoader(),
                new Class[]{restEasyService, service},
                new LeaveDefaultsProxyHandler(resteasy));
        }
        log.info("Created api client {}/{} {} ({})", getClass().getSimpleName(), service.getSimpleName(), baseUrl, mediaType);
        return proxyErrorsAndCount(service, proxy, errorClass);
    }

    protected <T> T proxyErrorsAndCount(Class<T> service, T proxy) {
        return proxyErrors(service, proxyCounter(service, proxy));
    }

    protected <T> T proxyErrorsAndCount(Class<T> service, T proxy, Class<?> errorClass) {
        return proxyErrors(service, proxyCounter(service, proxy), errorClass);
    }

    protected <T> T proxyErrors(Class<T> service, T proxy) {
        return ErrorAspect.proxyErrors(log, AbstractApiClient.this::getInfo, service, proxy);
    }

    protected <T> T proxyErrors(Class<T> service, T proxy, Class<?> errorClass) {
        return ErrorAspect.proxyErrors(log, AbstractApiClient.this::getInfo, service, proxy, errorClass);
    }

    protected <T> T proxyCounter(Class<T> service, T proxy) {
        return CountAspect.proxyCounter(counter, countWindow, getObjectName(), service, proxy);
    }

    protected <T> T build(ClientHttpEngine engine, Class<T> service) {
        return build(engine, service, null);
    }


    protected <T, S> T build(ClientHttpEngine engine, Class<T> service,  Class<S> restEasyInterface) {
        return build(engine, service, restEasyInterface, null);
    }

    protected <T, S> T buildWithErrorClass(ClientHttpEngine engine, Class<T> service, Class<S> restEasyInterface, Class<?> errorClass) {
        return build(engine, service, restEasyInterface, errorClass);
    }

    protected <T> T buildWithErrorClass(ClientHttpEngine engine, Class<T> service, Class<?> errorClass) {
        return buildWithErrorClass(engine, service, null, errorClass);
    }
    protected <T> T build(Class<T> service) {
        return build(getClientHttpEngine(), service);
    }

    private <T> T buildResteasy(ClientHttpEngine engine, Class<T> service) {

        return getTarget(engine)
            .proxyBuilder(service)
            .defaultConsumes(MediaType.APPLICATION_XML)
            .defaultProduces(MediaType.APPLICATION_XML)
            .build();
    }

    protected ResteasyClientBuilder resteasyClientBuilder(ClientHttpEngine engine) {
        ResteasyClientBuilder builder =
            new ResteasyClientBuilder()
                .httpEngine(engine)
                .register(JacksonContextResolver.class);
        builder.register(new AcceptRequestFilter(mediaType));
        builder.register(new AcceptLanguageRequestFilter(acceptableLanguages));
        buildResteasy(builder);
        return builder;
    }

    protected abstract void buildResteasy(ResteasyClientBuilder builder);

    protected final ResteasyWebTarget getTarget(ClientHttpEngine engine) {
        return resteasyClientBuilder(engine)
            .build()
            .target(baseUrl);
    }

    protected String getInfo() {
        return getBaseUrl() + "/";
    }

    public final String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getCounts() {
        return getCountMap()
            .entrySet()
            .stream()
            .map(Objects::toString)
            .collect(Collectors.joining("\n"));
    }

    @Override
    public long getCount(String method) {
        return getCountMap()
            .entrySet()
            .stream()
            .filter(e -> e.getKey().equals(method))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(0L);
    }

    private Map<String, Long> getCountMap() {
        return counter.entrySet()
            .stream()
            .collect(Collectors.toMap(e -> methodToString(e.getKey()), e -> e.getValue().getCount()));
    }

    @Override
    public long getTotalCount() {
        return counter.values().stream()
            .mapToLong(Counter::getCount)
            .sum();
    }

    @Override
    public double getRate() {
        return counter.values().stream()
            .mapToDouble(Counter::getRate)
            .sum();
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

    static String methodToString(Method m) {
        return m.getDeclaringClass().getSimpleName() + "." + m.getName();
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
        log.debug("Watching idle connections in {}", connectionManager);
        GUARD.connectionManagers.add(connectionManager);
        connectionManagers.add(connectionManager);
        if (connectionGuardThread == null) {
            GUARD.start();
            connectionGuardThread = THREAD_FACTORY.newThread(GUARD);
            connectionGuardThread.start();
        }
    }

    private synchronized void unwatchIdleConnections(PoolingHttpClientConnectionManager connectionManager) {
        log.debug("Unwatching idle connections in {}", connectionManager);
        GUARD.connectionManagers.remove(connectionManager);
        if (GUARD.connectionManagers.isEmpty()) {
            connectionGuardThread.interrupt();
            GUARD.shutdown();
            connectionGuardThread = null;
        }
    }

    private static class ConnectionGuard implements Runnable {

        private boolean shutdown = false;
        private List<HttpClientConnectionManager> connectionManagers = new ArrayList<>();

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
                        for (HttpClientConnectionManager connectionManager : connectionManagers) {
                            connectionManager.closeExpiredConnections();
                        }
                    }
                } catch (InterruptedException ignored) {
                    log.debug(ignored.getMessage());
                }
            }
            log.info("Shut down connection guard");
        }
    }
}

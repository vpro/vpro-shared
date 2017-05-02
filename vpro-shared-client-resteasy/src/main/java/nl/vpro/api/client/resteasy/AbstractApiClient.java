/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.cache.Cache;
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
import org.jboss.resteasy.client.jaxrs.cache.BrowserCache;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.client.jaxrs.engines.factory.ApacheHttpClient4EngineFactory;

import nl.vpro.jackson2.Jackson2Mapper;
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
  /*  static {
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
    }*/

    protected String baseUrl;

    private ClientHttpEngine clientHttpEngine;
    private ClientHttpEngine clientHttpEngineNoTimeout;

    private List<PoolingHttpClientConnectionManager> connectionManagers = new ArrayList<>();
    private boolean shutdown = false;
    private boolean trustAll = false;

    Duration connectionRequestTimeout;
    Duration connectTimeout;
    Duration socketTimeout;

    private Integer maxConnections;
    private Integer maxConnectionsPerRoute;
    Duration connectionInPoolTTL;
    protected final Map<String, Counter> counter = new HashMap<>();
    private Duration countWindow = Duration.ofHours(24);
    private Integer bucketCount = 24;

    private Duration warnThreshold = Duration.ofMillis(100);

    private List<Locale> acceptableLanguages = new ArrayList<>();

    private MediaType accept;

    private MediaType contentType;

    private BrowserCache resteasyBrowserCache;

    private Instant initializationInstant = Instant.now();

    private String mbeanName = null;


    @Getter
    private Jackson2Mapper objectMapper = Jackson2Mapper.getLenientInstance();

    protected AbstractApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
        List<Locale> acceptableLanguages,
        MediaType accept,
        MediaType contentType,
        Boolean trustAll,
        Jackson2Mapper objectMapper,
        String mbeanName
        ) {

        this.connectionRequestTimeout = connectionRequestTimeout;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.maxConnections = maxConnections;
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        this.connectionInPoolTTL = connectionInPoolTTL;
        setBaseUrl(baseUrl);
        this.countWindow = countWindow == null ? this.countWindow : countWindow;
        this.bucketCount = bucketCount == null ? this.bucketCount : bucketCount;
        this.warnThreshold = warnThreshold == null ? this.warnThreshold : warnThreshold;
        this.acceptableLanguages = acceptableLanguages;
        this.accept = accept;
        this.contentType = contentType;
        if (trustAll != null) {
            setTrustAll(trustAll);
        }
        this.objectMapper = objectMapper == null ? Jackson2Mapper.getLenientInstance() : objectMapper;
        this.mbeanName = mbeanName;
        registerBean();
    }

    @Deprecated
    protected AbstractApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
        List<Locale> acceptableLanguages,
        MediaType accept,
        Boolean trustAll
    ) {
        this(baseUrl, connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL, countWindow, bucketCount, warnThreshold, acceptableLanguages, accept, null, trustAll, null, null);

    }

    /**
     * @deprecated Will be dropped soon
     */
    @Deprecated
    protected AbstractApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        Duration warnThreshold,
        List<Locale> acceptableLanguages,
        MediaType accept,
        Boolean trustAll
    ) {
        this(baseUrl, connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL, countWindow, null, warnThreshold, acceptableLanguages, accept, null, trustAll, Jackson2Mapper.getLenientInstance(), null);
    }

    /**
     * @deprecated Will be dropped soon
     */
    @Deprecated
    protected AbstractApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        List<Locale> acceptableLanguages,
        MediaType accept,
        Boolean trustAll
    ) {
        this(baseUrl, connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL, countWindow, null, null, acceptableLanguages, accept, null, trustAll, Jackson2Mapper.getLenientInstance(), null);
    }

    protected AbstractApiClient(String baseUrl, Integer connectionTimeout, Integer maxConnections, Integer maxConnectionsPerRoute, Integer connectionInPoolTTL) {
        this(baseUrl,
            Duration.ofMillis(connectionTimeout == null ? -1 : connectionTimeout),
            Duration.ofMillis(connectionTimeout == null ? -1 : connectionTimeout),
            Duration.ofMillis(connectionTimeout == null ? -1 : connectionTimeout),
            maxConnections,
            maxConnectionsPerRoute,
            connectionInPoolTTL == null ? null : Duration.ofMillis(connectionInPoolTTL),
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            Jackson2Mapper.getLenientInstance(),
            null
        );
    }

    protected AbstractApiClient(String baseUrl, Integer connectionTimeout) {
        this(baseUrl, connectionTimeout, 0, 0, null);
    }

    public synchronized void invalidate() {
        counter.values().forEach(Counter::shutdown);
        counter.clear();
        this.initializationInstant = Instant.now();
        this.clientHttpEngine = null;
    }

    @Override
    public String getInitializationInstant() {
        return initializationInstant.toString();
    }

    @Override
    public String getConnectionRequestTimeout() {
        return String.valueOf(connectionRequestTimeout);
    }

    @Override
    public synchronized void setConnectionRequestTimeout(String connectionRequestTimeout) {
        Duration toSet = TimeUtils.parseDuration(connectionRequestTimeout).orElse(null);
        if (! Objects.equals(this.connectionRequestTimeout, toSet)) {
            this.connectionRequestTimeout = toSet;
            invalidate();
        }
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
        Duration toSet = TimeUtils.parseDuration(socketTimeout).orElse(null);
        if (! Objects.equals(this.socketTimeout, toSet)) {
            this.socketTimeout = toSet;
            invalidate();
        }
    }

    public MediaType getAccept() {
        return accept;
    }

    public void setAccept(MediaType mediaType) {
        this.accept = mediaType;
        this.invalidate();
    }


    public MediaType getContentType() {
        return contentType;
    }

    public void setContentType(MediaType mediaType) {
        this.contentType = mediaType;
        this.invalidate();
    }

    public void setTrustAll(boolean b) {
        this.trustAll = b;
        if (trustAll) {
            XTrustProvider.install();
        }
        invalidate();
    }


    public void setObjectMapper(Jackson2Mapper objectMapper) {
        this.objectMapper = objectMapper;
        invalidate();
    }

    private void registerBean() {
        registerBean(getObjectName(), this);
    }

    protected static synchronized void registerBean(ObjectName name, Object object) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            unregister(name);
            mbs.registerMBean(object, name);
        } catch (NotCompliantMBeanException | MBeanRegistrationException | InstanceAlreadyExistsException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected static void unregister(ObjectName name) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            if (mbs.isRegistered(name)) {
                log.info("Unregistering mbean {}", name);
                try {
                    mbs.unregisterMBean(name);
                } catch (InstanceNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.debug("Not registered");
            }
        } catch (MBeanRegistrationException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected ObjectName getObjectName() {
        try {
            if (mbeanName == null) {
                mbeanName = getClass().getSimpleName();
            }
            return new ObjectName("nl.vpro.api.client:type=" + mbeanName);

        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Checks wether supplier has null, if so produces it while locking, so it happens only on
     */
    protected <T> T produceIfNull(Supplier<T> supplier, Supplier<T> producer) {
        T t = supplier.get();
        if (t == null) {
            synchronized (this) {
                T found = supplier.get();
                if (found == null) {
                    t = producer.get();
                } else {
                    t = found;
                }
            }
        }
        return t;

    }

    private HttpClient getHttpClient(
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer  maxConnectionsPerRoute,
        Duration connectionInPoolTTL) {
        SocketConfig socketConfig = SocketConfig.custom()
            .setTcpNoDelay(true)
            .setSoKeepAlive(true)
            .setSoReuseAddress(true)
            .build();

        PoolingHttpClientConnectionManager connectionManager = null;
        if (maxConnections == null) {
            maxConnections = 0;
        }
        if (maxConnectionsPerRoute == null) {
            maxConnectionsPerRoute = 0;
        }
        if (connectionInPoolTTL != null) {
            connectionManager = new PoolingHttpClientConnectionManager(connectionInPoolTTL.toMillis(), TimeUnit.MILLISECONDS);
            if (maxConnections > 0) {
                connectionManager.setMaxTotal(maxConnections);
            }
            if (maxConnectionsPerRoute > 0) {
                connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
            }
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
            clientHttpEngine = ApacheHttpClient4EngineFactory.create(
                getHttpClient(connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL));

        }
        return clientHttpEngine;
    }

    public synchronized ClientHttpEngine getClientHttpEngineNoTimeout() {
        if (clientHttpEngineNoTimeout == null) {
            clientHttpEngineNoTimeout = ApacheHttpClient4EngineFactory.create(
                getHttpClient(connectionRequestTimeout, connectTimeout, null, 3, 3, null)
            );
        }
        return clientHttpEngineNoTimeout;
    }

    @Override
    public int getMaxConnections() {
        return maxConnections;
    }

    @Override
    public void setMaxConnections(int maxConnections) {
        if (this.maxConnections != maxConnections) {
            this.maxConnections = maxConnections;
            invalidate();
        }
    }

    @Override
    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    @Override
    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        if (this.maxConnectionsPerRoute != maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            invalidate();
        }
    }

    public Duration getConnectionInPoolTTL() {
        return connectionInPoolTTL;
    }

    public void setConnectionInPoolTTL(Duration connectionInPoolTTL) {
        this.connectionInPoolTTL = connectionInPoolTTL;
        clientHttpEngine = null;
        invalidate();
    }

    @Override
    public String getCountWindowString() {
        return countWindow.toString();
    }
    @Override
    public void setCountWindowString(String countWindow) {
        Duration toSet = TimeUtils.parseDuration(countWindow).orElse(Duration.ofSeconds(30));
        setCountWindow(toSet);
    }

    public void setCountWindow(Duration countWindow) {
        if (!Objects.equals(countWindow, this.countWindow)) {
            this.countWindow = countWindow;
            invalidate();
        }
    }

    @Override
    public Integer getBucketCount() {
        return bucketCount;
    }

    @Override
    public void setBucketCount(Integer bucketCount) {
        if (! Objects.equals(this.bucketCount, bucketCount)) {
            this.bucketCount = bucketCount;
            invalidate();
        }
    }

    @Override
    public String getWarnThresholdString() {
        return warnThreshold.toString();
    }

    @Override
    public void setWarnThresholdString(String warnThreshold) {
        setWarnThreshold(TimeUtils.parseDuration(warnThreshold).orElse(Duration.ofMillis(100)));
    }

    public Duration getWarnThreshold() {
        return warnThreshold;
    }

    public void setWarnThreshold(Duration warnThreshold) {
        this.warnThreshold = warnThreshold;
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
        log.info("Created api client {}/{} {} ({})", getClass().getSimpleName(), service.getSimpleName(), baseUrl, accept);
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
        return CountAspect.proxyCounter(counter, countWindow, bucketCount, getObjectName(), service, proxy, log, warnThreshold);
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
        ResteasyClientBuilder builder = new ResteasyClientBuilder()
            .httpEngine(engine);
        builder.register(new JacksonContextResolver(objectMapper));
        builder.register(new AcceptRequestFilter(accept));
        builder.register(new AcceptLanguageRequestFilter(acceptableLanguages));
        builder.register(new CountFilter(getObjectName(), log));

        BrowserCacheFeature browserCacheFeature = new BrowserCacheFeature();

        if (this.resteasyBrowserCache != null) {
            browserCacheFeature.setCache(this.resteasyBrowserCache);
        }
        builder.register(browserCacheFeature);
        buildResteasy(builder);
        if (browserCacheFeature.getCache() != this.resteasyBrowserCache) {
            this.resteasyBrowserCache = browserCacheFeature.getCache();
            log.info("Set browser cache to {}", this.resteasyBrowserCache);
        } else {
            log.debug("Browser cache was already set to be {}", this.resteasyBrowserCache);
        }
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

    @Override
    public final String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void setBaseUrl(String url) {
        url = url == null ? null : (url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
        if (! Objects.equals(url, this.baseUrl)) {
            this.baseUrl = url;
            invalidate();
        }
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
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCount()));
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


    public BrowserCache getBrowserCache() {
        return resteasyBrowserCache;
    }

    public void setBrowserCache(Cache<?, ?> browserCache) {
        setBrowserCache(new JavaxBrowserCache((Cache<String, Map<String, BrowserCache.Entry>>) browserCache));
    }
    public void setBrowserCache(BrowserCache browserCache) {
        this.resteasyBrowserCache = browserCache;
        invalidate();
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

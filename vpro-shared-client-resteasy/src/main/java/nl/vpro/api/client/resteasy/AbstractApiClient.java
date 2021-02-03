/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jmx.CountAspect;
import nl.vpro.jmx.Counter;
import nl.vpro.jmx.MBeans;
import nl.vpro.rs.client.*;
import nl.vpro.util.LeaveDefaultsProxyHandler;
import nl.vpro.util.ThreadPools;
import nl.vpro.util.TimeUtils;
import nl.vpro.util.XTrustProvider;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public abstract class AbstractApiClient implements AbstractApiClientMXBean, AutoCloseable {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static Thread connectionGuardThread;
    private static final ThreadFactory THREAD_FACTORY = ThreadPools.createThreadFactory("API Client purge idle connections", true, Thread.NORM_PRIORITY);
    private static final ConnectionGuard GUARD = new ConnectionGuard();


    protected String baseUrl;

    private ClientHttpEngine clientHttpEngine;
    private ClientHttpEngine clientHttpEngineNoTimeout;

    private final List<PoolingHttpClientConnectionManager> connectionManagers = new ArrayList<>();
    private boolean shutdown = false;
    protected boolean trustAll = false;

    protected Duration connectionRequestTimeout;
    protected Duration connectTimeout;
    protected Duration socketTimeout;

    protected Integer maxConnections;
    protected Integer maxConnectionsPerRoute;
    protected Integer maxConnectionsNoTimeout;
    protected Integer maxConnectionsPerRouteNoTimeout;


    protected Duration connectionInPoolTTL;
    protected Duration validateAfterInactivity;

    protected final Map<String, Counter> counter = new ConcurrentHashMap<>();
    protected Duration countWindow = Duration.ofHours(24);
    protected Integer bucketCount = 24;

    protected Duration warnThreshold = Duration.ofMillis(100);

    protected List<Locale> acceptableLanguages = new ArrayList<>();

    protected MediaType accept;

    protected MediaType contentType;

    private BrowserCache resteasyBrowserCache;

    private Instant initializationInstant = Instant.now();

    protected String mbeanName = null;

    protected boolean registerMBean = false;

    @Getter
    protected Jackson2Mapper objectMapper = Jackson2Mapper.getLenientInstance();

    protected ClassLoader classLoader;

    protected final String userAgent;


    @Deprecated
    protected AbstractApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Integer maxConnectionsNoTimeout,
        Integer maxConnectionsPerRouteNoTimeout,
        Duration connectionInPoolTTL,
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
        List<Locale> acceptableLanguages,
        MediaType accept,
        MediaType contentType,
        Boolean trustAll,
        Jackson2Mapper objectMapper,
        String mbeanName,
        ClassLoader classLoader,
        String userAgent,
        Boolean registerMBean
    ) {
        this(baseUrl, connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, maxConnectionsNoTimeout, maxConnectionsPerRouteNoTimeout, connectionInPoolTTL, null, countWindow, bucketCount, warnThreshold, acceptableLanguages, accept, contentType, trustAll, objectMapper, mbeanName, classLoader, userAgent, registerMBean);
    }

    protected AbstractApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Integer maxConnectionsNoTimeout,
        Integer maxConnectionsPerRouteNoTimeout,
        Duration connectionInPoolTTL,
        Duration validateAfterInactivity,
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
        List<Locale> acceptableLanguages,
        MediaType accept,
        MediaType contentType,
        Boolean trustAll,
        Jackson2Mapper objectMapper,
        String mbeanName,
        ClassLoader classLoader,
        String userAgent,
        Boolean registerMBean
        ) {

        this.connectionRequestTimeout = connectionRequestTimeout;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.maxConnections = maxConnections;
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        this.maxConnectionsNoTimeout = maxConnectionsNoTimeout == null ? 3 : maxConnectionsNoTimeout;
        this.maxConnectionsPerRouteNoTimeout = maxConnectionsPerRouteNoTimeout == null ? 3 : maxConnectionsPerRouteNoTimeout;
        this.connectionInPoolTTL = connectionInPoolTTL;
        this.validateAfterInactivity = validateAfterInactivity;
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
        this.classLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        this.userAgent = userAgent == null ? getUserAgent(getClass().getSimpleName(), getVersion("vpro.shared.version", this.classLoader)) : userAgent;
        log.info("Using class loader {}, user agent {}", this.classLoader, this.userAgent);
        this.registerMBean = registerMBean == null || this.registerMBean;
        if (this.registerMBean) {
            registerBean();
        }
    }

    @SneakyThrows
    protected String getVersion(String prop, ClassLoader loader) {
        Properties properties = new Properties();
        try {
            URL resource = loader.getResource("/maven.properties");
            if (resource == null) {
                log.warn("No maven.properties found");
            } else {
                properties.load(resource.openStream());
            }
        } catch (Exception e) {
            log.warn(e.getClass() + " " + e.getMessage());
        }
        return properties.getProperty(prop);
    }

    public static String getUserAgent(final String name, final String version) {
        final String javaVersion = System.getProperty("java.version");
        return String.format("%s/%s (Java/%s)", name, version, javaVersion);
    }


    @Override
    public synchronized void invalidate() {
        counter.values().forEach(Counter::shutdown);
        counter.clear();
        this.initializationInstant = Instant.now();
        closeClients();
    }

    @Override
    public String getInitializationInstant() {
        return initializationInstant.toString();
    }

    @Override
    public synchronized String getConnectionRequestTimeout() {
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
    public synchronized String getConnectTimeout() {
        return String.valueOf(connectTimeout);
    }

    @Override
    public synchronized void setConnectTimeout(String connectTimeout) {
        Duration parsed = TimeUtils.parseDuration(connectTimeout).orElse(null);
        if (! Objects.equals(parsed, this.connectTimeout)) {
            this.connectTimeout = parsed;
            invalidate();
        }
    }

    @Override
    public synchronized String getSocketTimeout() {
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

    @Override
    public final String test(String arg) {
        StringBuilder builder = new StringBuilder();
        appendTestResult(builder, arg);
        return builder.toString();

    }

    protected void appendTestResult(StringBuilder builder, String arg) {
        builder.append(this).append("\n");
        builder.append("initialized at: ").append(getInitializationInstant());
        builder.append("\n");
    }

    public MediaType getAccept() {
        return accept;
    }

    public void setAccept(MediaType mediaType) {
        if (this.accept != mediaType) {
            this.accept = mediaType;
            this.invalidate();
        }
    }


    public MediaType getContentType() {
        return contentType;
    }

    public void setContentType(MediaType mediaType) {
        if (this.contentType != mediaType) {
            this.contentType = mediaType;
            this.invalidate();
        }
    }

    public void setTrustAll(boolean b) {
        if (this.trustAll != b) {
            this.trustAll = b;
            if (trustAll) {
                XTrustProvider.install();
            }
            invalidate();
        }
    }


    public void setObjectMapper(Jackson2Mapper objectMapper) {
        if (! Objects.equals(this.objectMapper, objectMapper)) {
            this.objectMapper = objectMapper;
            invalidate();
        }
    }

    private void registerBean() {
        MBeans.registerBean(getObjectName(), this);
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
     * Checks wether supplier has null, if so produces it while locking, so it happens only once
     */
    protected <T> T produceIfNull(Supplier<T> supplier, Supplier<T> producer) {
        T t = supplier.get();
        if (t == null) {
            log.debug("Not found");
            synchronized (this) {
                T found = supplier.get();
                if (found == null) {
                    log.debug("Still not found,  now producing");
                    t = producer.get();
                } else {
                    log.debug("Found after all");
                    t = found;
                }
            }
        } else {
            log.debug("Using previously produced instance");
        }
        return t;

    }

    private CloseableHttpClient getHttpClient(
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration validateAfterInactivity) {
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
        // Why is connectionInPoolTTL the criterion to check?
        if (connectionInPoolTTL != null) {
            connectionManager = new PoolingHttpClientConnectionManager(connectionInPoolTTL.toMillis(), TimeUnit.MILLISECONDS);
            if (validateAfterInactivity != null) {
                connectionManager.setValidateAfterInactivity((int) validateAfterInactivity.toMillis());
            }

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
            .setUserAgent(userAgent)
            .setRetryHandler((exception, executionCount, context) -> {
                if (exception instanceof NoHttpResponseException && executionCount < 3) {
                    log.warn("{} Retrying ({})", exception.getMessage(), executionCount);
                    return true;
                }
                return false;

            })
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

        return client.build();
    }

    public synchronized ClientHttpEngine getClientHttpEngine() {
        if (clientHttpEngine == null) {
            clientHttpEngine = ResteasyHelper.createApacheHttpClient(getHttpClient(connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL, validateAfterInactivity), false);

        }
        return clientHttpEngine;
    }

    public synchronized ClientHttpEngine getClientHttpEngineNoTimeout() {
        if (clientHttpEngineNoTimeout == null) {
            clientHttpEngineNoTimeout = ResteasyHelper.createApacheHttpClient(getHttpClient(connectionRequestTimeout, connectTimeout, null, maxConnectionsNoTimeout, maxConnectionsPerRouteNoTimeout, null, validateAfterInactivity), false);
        }
        return clientHttpEngineNoTimeout;
    }


    @Override
    public Integer getMaxConnections() {
        return maxConnections;
    }

    @Override
    public void setMaxConnections(Integer maxConnections) {
        if (! Objects.equals(this.maxConnections, maxConnections)) {
            this.maxConnections = maxConnections;
            invalidate();
        }
    }

    @Override
    public Integer getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    @Override
    public void setMaxConnectionsPerRoute(Integer maxConnectionsPerRoute) {
        if (! Objects.equals(this.maxConnectionsPerRoute, maxConnectionsPerRoute)) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            invalidate();
        }
    }


    @Override
    public Integer getMaxConnectionsNoTimeout() {
        return maxConnectionsNoTimeout;
    }

    @Override
    public void setMaxConnectionsNoTimeout(Integer maxConnectionsNoTimeout) {
        if (! Objects.equals(this.maxConnectionsNoTimeout, maxConnectionsNoTimeout)) {
            this.maxConnectionsNoTimeout = maxConnectionsNoTimeout;
            invalidate();
        }
    }

    @Override
    public Integer getMaxConnectionsPerRouteNoTimeout() {
        return maxConnectionsPerRouteNoTimeout;
    }

    @Override
    public void setMaxConnectionsPerRouteNoTimeout(Integer maxConnectionsPerRouteNoTimeout) {
        if (! Objects.equals(this.maxConnectionsPerRouteNoTimeout, maxConnectionsPerRouteNoTimeout)) {
            this.maxConnectionsPerRouteNoTimeout = maxConnectionsPerRouteNoTimeout;
            invalidate();
        }
    }

    public Duration getConnectionInPoolTTL() {
        return connectionInPoolTTL;
    }

    public void setConnectionInPoolTTL(Duration connectionInPoolTTL) {
        if (!Objects.equals(this.connectionInPoolTTL, connectionInPoolTTL)) {
            this.connectionInPoolTTL = connectionInPoolTTL;
            clientHttpEngine = null;
            invalidate();
        }
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
        if (! Objects.equals(warnThreshold, this.warnThreshold)) {
            this.warnThreshold = warnThreshold;
            invalidate();
        }
    }

    public List<Locale> getAcceptableLanguages() {
        return acceptableLanguages;
    }

    public void setAcceptableLanguages(List<Locale> acceptableLanguages) {
        if (! Objects.equals(acceptableLanguages, this.acceptableLanguages)) {
            this.acceptableLanguages = acceptableLanguages;
            invalidate();
        }
    }

    @SuppressWarnings("unchecked")
    protected <T, S, E> T build(
        ClientHttpEngine engine,
        Class<T> service,
        Class<S> restEasyService,
        Class<E> errorClass,
        Consumer<ResteasyClientBuilder> buildFurther) {
        T proxy;
        if (restEasyService == null) {
            proxy = buildResteasy(engine, service, buildFurther);
        } else {
            S resteasy = buildResteasy(engine, restEasyService, buildFurther);
            proxy = (T) Proxy.newProxyInstance(
                restEasyService.getClassLoader(),
                new Class[]{restEasyService, service},
                new LeaveDefaultsProxyHandler(resteasy));
        }
        log.info("Created api client {}/{} {} ({})", getClass().getSimpleName(), service.getSimpleName(), baseUrl, accept);
        return proxyErrorsAndCount(service, proxy, errorClass);
    }

    protected <T> T proxyErrorsAndCount(Class<T> service, T proxy) {
        return proxyErrors(service, proxyCounter(service, proxy));
    }

    protected <T, E> T proxyErrorsAndCount(Class<T> service, T proxy, Class<E> errorClass) {
        return proxyErrors(service, proxyCounter(service, proxy), errorClass);
    }

    protected <T> T proxyErrors(Class<T> service, T proxy) {
        return proxyErrors(service, proxy, null);
    }

    /**
     *
     * @param service The service interface
     * @param proxy The current proxy object for the service interface
     * @param errorClass The class representing an error
     * @param <T> The type of the service interface
     * @return  A new proxy, with the functionality of {@link ErrorAspect} added.
     */
    protected <T, E> T proxyErrors(Class<T> service, T proxy, Class<E> errorClass) {
        return ErrorAspect.proxyErrors(log, AbstractApiClient.this::getInfo, service, proxy, errorClass);
    }

      /**
     *
     * @param service The service interface
     * @param proxy The current proxy object for the service interface
     * @param <T> The type of the service interface
     * @return  A new proxy, with the functionality of {@link nl.vpro.jmx.CountAspect} added.
     */
    protected <T> T proxyCounter(Class<T> service, T proxy) {
        return CountAspect.proxyCounter(counter, countWindow, bucketCount,
                registerMBean ? getObjectName() : null, service, proxy, log, warnThreshold);
    }

    protected <T> T build(ClientHttpEngine engine, Class<T> service, Consumer<ResteasyClientBuilder> buildFurther) {
        return build(engine, service, null, buildFurther);
    }

    protected <T> T build(ClientHttpEngine engine, Class<T> service) {
        return build(engine, service, null, null);
    }

    protected <T, S> T build(ClientHttpEngine engine, Class<T> service,  Class<S> restEasyInterface, Consumer<ResteasyClientBuilder> buildFurther) {
        return build(engine, service, restEasyInterface, null, buildFurther);
    }

    protected <T, S> T buildWithErrorClass(
        ClientHttpEngine engine,
        Class<T> service,
        Class<S> restEasyInterface,
        Class<?> errorClass,
        Consumer<ResteasyClientBuilder> buildFurther) {
        return build(engine, service, restEasyInterface, errorClass, buildFurther);
    }


    protected <T, S> T buildWithErrorClass(
        ClientHttpEngine engine,
        Class<T> service,
        Class<S> restEasyInterface,
        Class<?> errorClass) {
        return build(engine, service, restEasyInterface, errorClass, null);
    }

    protected <T> T buildWithErrorClass(ClientHttpEngine engine, Class<T> service, Class<?> errorClass, Consumer<ResteasyClientBuilder> buildFurther) {
        return buildWithErrorClass(engine, service, null, errorClass, buildFurther);
    }

    protected <T> T buildWithErrorClass(ClientHttpEngine engine, Class<T> service, Class<?> errorClass) {
        return buildWithErrorClass(engine, service, null, errorClass, null);
    }

    protected <T> T build(Class<T> service, Consumer<ResteasyClientBuilder> buildFurther) {
        return build(getClientHttpEngine(), service, buildFurther);
    }

    protected <T> T build(Class<T> service) {
        return build(service, null);
    }

    private <T> T buildResteasy(ClientHttpEngine engine, Class<T> service, Consumer<ResteasyClientBuilder> buildFurther) {

        return getTarget(engine, buildFurther)
            .proxyBuilder(service)
            .classloader(classLoader)
            .defaultConsumes(MediaType.APPLICATION_XML)
            .defaultProduces(MediaType.APPLICATION_XML)
            .build();
    }

    protected ResteasyClientBuilder resteasyClientBuilder(ClientHttpEngine engine, Consumer<ResteasyClientBuilder> buildFurther) {
        ResteasyClientBuilder builder = defaultResteasyClientBuilder(engine);
        if (buildFurther != null) {
            buildFurther.accept(builder);
        }
        buildResteasy(builder);
        return builder;
    }

     protected ResteasyClientBuilder defaultResteasyClientBuilder(ClientHttpEngine engine) {
        ResteasyClientBuilder builder = ResteasyHelper.clientBuilder()
            .httpEngine(engine);
        builder.register(new JacksonContextResolver(objectMapper));
        builder.register(new AcceptRequestFilter(accept));
        builder.register(new AcceptLanguageRequestFilter(acceptableLanguages));
        builder.register(new CountFilter(log));

        BrowserCacheFeature browserCacheFeature = new BrowserCacheFeature();

        if (this.resteasyBrowserCache != null) {
            browserCacheFeature.setCache(this.resteasyBrowserCache);
        }
        builder.register(browserCacheFeature);
        if (browserCacheFeature.getCache() != this.resteasyBrowserCache) {
            this.resteasyBrowserCache = browserCacheFeature.getCache();
            log.info("Set browser cache to {}", this.resteasyBrowserCache);
        } else {
            log.debug("Browser cache was already set to be {}", this.resteasyBrowserCache);
        }
        return builder;
    }

    /**
     * For further building the client you can override this method.
     * If you need more control you can also use the several 'buildFurther' arguments.
     */
    protected void buildResteasy(ResteasyClientBuilder builder) {

    }

    protected final ResteasyWebTarget getTarget(ClientHttpEngine engine, Consumer<ResteasyClientBuilder> buildFurther) {
        return resteasyClientBuilder(engine, buildFurther)
            .build()
            .target(baseUrl);
    }


    protected final ResteasyWebTarget getTarget(ClientHttpEngine engine) {
        return getTarget(engine, null);
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
        if (! Objects.equals(browserCache, this.resteasyBrowserCache)) {
            this.resteasyBrowserCache = browserCache;
            invalidate();
        }
    }

    @PreDestroy
    public void shutdown() {
        close();
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    static String methodToString(Method m) {
        return m.getDeclaringClass().getSimpleName() + "." + m.getName();
    }

    private static class MyConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {

            HttpRequestWrapper wrapper = (HttpRequestWrapper)context.getAttribute(HttpClientContext.HTTP_REQUEST);
            if(wrapper.getURI().getPath().endsWith("/media/changes")) {
                // 30 minutes
                return 30L * 60 * 1000;
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
            return 60L * 1000;
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


    @Override
    public synchronized void close() {
        closeClients();
        if(!shutdown) {
            shutdown = true;
            for (PoolingHttpClientConnectionManager connectionManager : connectionManagers) {
                unwatchIdleConnections(connectionManager);
            }
        }
        invalidate();
    }

    protected synchronized void closeClients() {
        if (clientHttpEngine != null) {
            clientHttpEngine.close();
            clientHttpEngine = null;
        }
        if (clientHttpEngineNoTimeout != null) {
            clientHttpEngineNoTimeout.close();
            clientHttpEngineNoTimeout = null;
        }
    }


    @Slf4j
    private static class ConnectionGuard implements Runnable {

        private boolean shutdown = false;
        private final List<HttpClientConnectionManager> connectionManagers = new ArrayList<>();

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
                } catch (InterruptedException ie) {
                    log.debug(ie.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                }
            }
            log.info("Shut down connection guard");
        }
    }
}

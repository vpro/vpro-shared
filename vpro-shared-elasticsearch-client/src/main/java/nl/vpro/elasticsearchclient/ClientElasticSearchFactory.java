package nl.vpro.elasticsearchclient;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.LoggerFactory;

import nl.vpro.jmx.MBeans;
import nl.vpro.logging.simple.SimpleLogger;
import nl.vpro.util.TimeUtils;


/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
@Slf4j
@Data
public class ClientElasticSearchFactory implements AsyncESClientFactory, ClientElasticSearchFactoryMXBean {

    private static int instances = 0;

    private String clusterName;
    private String unicastHosts;

    private String basicUser;
    private String basicPassword;


    private Duration socketTimeout = Duration.ofSeconds(60);
    private Duration connectionTimeout = Duration.ofSeconds(5);
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration maxRetryTimeout = Duration.ofSeconds(60);

    private RestClient client;

    private final Map<String, CompletableFuture<RestClient>> clients = new HashMap<>();

    private final int instance = instances++;

    private boolean registerMBean = true;

    private List<RestClientBuilder.HttpClientConfigCallback> clientConfigCallbacks;

    @PostConstruct
    @SneakyThrows
    public void init() {
        log.info("Found {}", this);
        if (registerMBean) {
            String name = instance + "-" + clusterName;
            MBeans.registerBean(this, name);
        }
    }


    @Override
    public String invalidate() {
        String keys = clients.keySet().toString();
        clients.clear();
        shutdown();
        return "Cleared " + keys;
    }

    @Override
    public String getClients() {
        return clients.entrySet().toString();

    }

    @Override
    public CompletableFuture<RestClient> clientAsync(
        @Nullable String logName, Consumer<RestClient> callback) {
        if (logName == null) {
            logName = "NULL";
        }
        SimpleLogger l = SimpleLogger.slfj4(LoggerFactory.getLogger(logName));

        CompletableFuture<RestClient> future = clients.computeIfAbsent(logName, (ln) -> {
            CompletableFuture<RestClient> result = createAndCheckClient(l);
            result.thenAccept(callback);
            return result;
        });
        return future;

    }


    private CompletableFuture<RestClient> createAndCheckClient(SimpleLogger l) {
        if (createClientIfNeeded()) {
            CompletableFuture<RestClient> future = new CompletableFuture<>();
            HttpHost[] hosts = getHosts();
            IndexHelper.getClusterName(l, client)
                .whenComplete((foundClusterName, exception) -> {
                    if (exception != null) {
                        future.completeExceptionally(exception);
                    }
                    if (clusterName != null && !clusterName.equals(foundClusterName)) {
                        future.completeExceptionally(
                            new IllegalStateException(Arrays.toString(hosts) + ": Connected to wrong cluster ('" + foundClusterName + "' != '" + clusterName + "')"));
                        return;
                    }
                    future.complete(client);
                    if (l.isInfoEnabled()) {
                        l.info("Connected to cluster '{}'", foundClusterName);
                    }
                });
            return future;
        } else {
            return CompletableFuture.completedFuture(client);
        }
    }

    private boolean createClientIfNeeded() {
        if (client == null) {
            HttpHost[] hosts = getHosts();
            final RestClientBuilder clientBuilder = RestClient
                .builder(hosts);

            client = clientBuilder
                .setHttpClientConfigCallback((hacb) -> {
                    if (clientConfigCallbacks != null) {
                        clientConfigCallbacks.forEach(a -> a.customizeHttpClient(hacb));
                    }
                    setupAuthorizationIfNecessary().customizeHttpClient(hacb);
                    return hacb;
                })
                .setRequestConfigCallback(
                    requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout((int) connectTimeout.toMillis())
                        .setSocketTimeout((int) socketTimeout.toMillis())
                        .setConnectionRequestTimeout((int) connectionTimeout.toMillis())
                )
                //.setMaxRetryTimeout((int) maxRetryTimeout.toMillis())
                .build();
            log.info("Created {}: {}", Arrays.asList(hosts), client);
            return true;
        } else {
            return false;
        }
    }


    protected HttpHost[] getHosts() {
        HttpHost[] httpHosts = Arrays.stream(unicastHosts.split(("\\s*,\\s*")))
            .filter(s -> !s.isEmpty())
            .map(s -> {
                if (! s.startsWith("http:") && ! s.startsWith("https:") && ! s.contains(":")) {
                    s = s + ":9200";
                }
                return s;
            })
            .map(HttpHost::create)
            .toArray(HttpHost[]::new);
        return httpHosts;
    }

    @Override
    public String toString() {
        return logString() + ":" + client;
    }

    @Override
    public String logString() {
        try {
            HttpHost[] hosts = getHosts();
            if (hosts == null) {
                throw new RuntimeException("No hosts");
            }
            if (hosts.length > 0) {
                return hosts[0].toString();

            } else {
                return unicastHosts;
            }
        } catch (Exception e) {
            log.error(e.getClass().getName() + ":" + e.getMessage());
            return unicastHosts;
        }
    }

    public void setSocketTimeoutDuration(String socketTimeout) {
        this.socketTimeout = TimeUtils.parseDuration(socketTimeout).orElse(this.socketTimeout);
        invalidate();
    }

    public void setConnectionTimeoutDuration(String connectionTimeout) {
        this.connectionTimeout = TimeUtils.parseDuration(connectionTimeout).orElse(this.connectionTimeout);
        invalidate();
    }

    public void setConnectTimeoutDuration(String connectTimeout) {
        this.connectTimeout = TimeUtils.parseDuration(connectTimeout).orElse(this.connectTimeout);
        invalidate();
    }

    public void setMaxRetryTimeoutDuration(String maxRetryTimeout) {
        this.maxRetryTimeout = TimeUtils.parseDuration(maxRetryTimeout).orElse(this.maxRetryTimeout);
        invalidate();
    }

    public void shutdown() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            client = null;
        }
    }

    protected RestClientBuilder.HttpClientConfigCallback setupAuthorizationIfNecessary() {
        return httpClientBuilder -> {
            if (StringUtils.isNotBlank(basicUser)) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(basicUser, basicPassword));

                httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
            }
            return httpClientBuilder;
        };
    }



    @Override
    public void close() {
        shutdown();
    }


}

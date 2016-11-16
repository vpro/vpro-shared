package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple http client wrapping exactly one external resource, keeping track of cache headers.
 * @author Michiel Meeuwissen
 * @since 0.37
 */
public class URLResource<T> {


    @SafeVarargs
    public static URLResource<Properties> properties(URI url, Consumer<Properties>... callbacks) {
        return new URLResource<>(url, PROPERTIES, new Properties(), callbacks);
    }

    @SafeVarargs
    public static URLResource<Map<String, String>> map(URI url, Consumer<Map<String, String>>... callbacks) {
        return new URLResource<>(url, MAP, new HashMap<>(), callbacks);
    }


    @SafeVarargs
    public static <S> URLResource<List<S>> beansFromProperties(Function<String, S> constructor, URI url, Consumer<List<S>>... callbacks) {
        return new URLResource<>(url, beansFromProperties(constructor), new ArrayList<>(), callbacks);
    }

    private static final int SC_OK = 200;
    private static final int SC_NOT_MODIFIED = 304;


    private static final Logger LOG = LoggerFactory.getLogger(URLResource.class);

    private Instant lastLoad = null;
    private Integer code = null;
    private final URI url;

    private Instant lastModified = null;
    private Instant expires = null;
    private Duration maxAge = Duration.of(1, ChronoUnit.HOURS);
    private Duration minAge = Duration.of(5, ChronoUnit.MINUTES);
    private Duration errorCache = Duration.of(1, ChronoUnit.MINUTES);


    private final Function<InputStream, T> reader;

    private T result;

    private long okCount = 0;
    private long notModifiedCount = 0;
    private long notCheckedCount = 0;
    private long checkedCount = 0;
    private long changesCount = 0;
    private long errorCount = 0;
    private boolean async = false;
    private T empty = null;

    private ScheduledFuture<?> future = null;
    private Consumer<T>[] callbacks;

    @Setter
    @Getter
    private Duration connectTimeout = Duration.ofMillis(500);
    @Setter
    @Getter
    private Duration readTimeout = Duration.ofMillis(500);


    @SafeVarargs
    public URLResource(URI url, Function<InputStream, T> reader, T empty, Consumer<T>... callbacks) {
        this.url = url;
        this.empty = empty;
        this.reader = reader;
        this.callbacks = callbacks;

    }

    @SafeVarargs
    public URLResource(URI url, Function<InputStream, T> reader, Consumer<T>... callbacks) {
        this(url, reader, null, callbacks);
    }


    public T get() {
        if (result == null) {
            if (async) {
                return empty;
            }
        }
        if (!async) {
            getCachedResource();
        }

        return result;
    }

    void getCachedResource() {
        if (result != null && lastLoad != null && lastLoad.plus(minAge).isAfter(Instant.now())) {
            notCheckedCount++;
            return;
        }
        checkedCount++;
        LOG.debug("Loading from {}", this.url);
        try {
            if (this.url.getScheme().equals("classpath")) {
                getCachedResource(this.url.toString().substring("classpath:".length() + 1));
            } else {
                URLConnection connection = openConnection();
                getCachedResource(connection);

            }
        } catch (java.net.UnknownHostException uhe) {
            errorCount++;
            LOG.warn(uhe.getClass().getName() + " " + uhe.getMessage());
        } catch (IOException e) {
            errorCount++;
            LOG.error(e.getMessage(), e);
        }
    }

    URLConnection openConnection() throws IOException {
        return url.toURL().openConnection();
    }

    void getCachedResource(String resource) {
        T newResult = reader.apply(getClass().getClassLoader().getResourceAsStream(resource));
        if (newResult != null) {
            if (result == null) {
                lastLoad = Instant.now();
                lastModified = Instant.now();
                LOG.info("Loaded {} from {}", newResult, this.url);
            } else {
                if (!Objects.equals(result, newResult)) {
                    LOG.info("Reloaded {} from {}", newResult, this.url);
                    lastModified = Instant.now();
                    changesCount++;
                }
                lastLoad = Instant.now();

            }
            result = newResult;
            callBack();
        } else {
            LOG.warn("Loading from {} resulted null", this.url);
        }
    }

    void callBack() {
        for (Consumer<T> callback : callbacks) {
            callback.accept(result);
        }
    }

    void getCachedResource(URLConnection connection) throws IOException {
        if (result != null && expires != null && Instant.now().isBefore(expires)) {
            LOG.debug("Not loading {} as it is not yet expired", url);
            return;
        }
        boolean httpUrl = connection instanceof HttpURLConnection;
        code = -1;
        if (httpUrl && lastModified != null) {
            if (lastLoad == null || lastLoad.isAfter(Instant.now().minus(maxAge))) {
                connection.setRequestProperty("If-Modified-Since", DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified.atOffset(ZoneOffset.UTC)));
            } else {
                LOG.debug("last load was pretty long ago, simply do a normal request");
            }
        }
        if (httpUrl) {
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            httpURLConnection.setConnectTimeout((int) connectTimeout.toMillis());
            httpURLConnection.setReadTimeout((int) readTimeout.toMillis());
            httpURLConnection.setInstanceFollowRedirects(true);

            try {
                code = httpURLConnection.getResponseCode();
            } catch (SocketTimeoutException ste) {
                LOG.warn(ste.getMessage());
                code = -1;
            }
        } else {
            code = SC_OK;
        }
        switch (code) {
            case SC_NOT_MODIFIED:
                LOG.debug("Not modified {}", url);
                notModifiedCount++;
                break;
            case SC_OK:
                okCount++;
                InputStream stream = connection.getInputStream();
                Instant prevMod = lastModified;
                lastModified = Instant.ofEpochMilli(connection.getHeaderFieldDate("Last-Modified", System.currentTimeMillis()));
                if (connection.getHeaderField("Expires") != null) {
                    expires = Instant.ofEpochMilli(connection.getHeaderFieldDate("Expires", System.currentTimeMillis()));
                }
                String cacheControl = connection.getHeaderField("Cache-Control");
                if (cacheControl != null) {
                    String[] split = cacheControl.split("\\s*,\\s*");
                    for (String s : split) {
                        if (s.startsWith("max-age")) {
                            String[] ma = s.split("\\s*[:=]\\s*", 2);
                            try {
                                if (ma.length == 2) {
                                    expires = Instant.now().plus(Duration.of(Integer.parseInt(ma[1]), ChronoUnit.SECONDS));
                                } else {
                                    LOG.warn("Could not parse " + s);
                                }
                            } catch (Exception e) {
                                LOG.warn("Could not parse " + s + " " + e.getMessage());
                            }
                        }
                    }
                }
                Instant maxExpires = Instant.now().plus(maxAge);
                if (expires != null && expires.isAfter(maxExpires)) {
                    LOG.info("Found expiry {} for {} truncated to {}", expires, url, maxExpires);
                    expires = maxExpires;
                }
                T newResult = reader.apply(stream);
                if (newResult != null) {
                    if (result == null) {
                        LOG.info("Loaded {} -> {}", url, lastModified);
                    } else {
                        LOG.info("Reloaded {}  as it is modified since {}  -> {}", url, prevMod, lastModified);
                    }
                    changesCount++;
                    result = newResult;
                    callBack();
                }
                stream.close();
                lastLoad = Instant.now();
                break;
            default:
                if (result == null) {
                    result = empty;
                }
                lastLoad = Instant.now();
                lastModified = null;
                errorCount++;
                expires = Instant.now().plus(errorCache);
                LOG.warn(code + ":" + url + ": (caching until " + expires + ")");


        }

    }


    public URI getUrl() {
        return url;
    }

    public Instant getLastLoad() {
        return lastLoad;
    }

    public Instant getLastModified() {
        return lastModified;
    }


    public long getOkCount() {
        return okCount;
    }

    public long getNotModifiedCount() {
        return notModifiedCount;
    }


    public long getChangesCount() {
        return changesCount;
    }

    public long getNotCheckedCount() {
        return notCheckedCount;
    }

    public long getCheckedCount() {
        return checkedCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public Integer getCode() {
        return code;
    }

    public Instant getExpires() {
        return expires;
    }

    public Duration getMaxAge() {
        return maxAge;
    }

    public URLResource<T> setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public Duration getMinAge() {
        return minAge;
    }

    public URLResource<T> setMinAge(Duration minAge) {
        this.minAge = minAge;
        return this;
    }

    public Duration getErrorCache() {
        return errorCache;
    }

    public URLResource<T> setErrorCache(Duration errorCache) {
        this.errorCache = errorCache;
        return this;
    }



    @SafeVarargs
    public final URLResource<T> setCallbacks(Consumer<T>... callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public URLResource<T> setAsync(boolean async) {
        this.async = async;
        if (this.async) {
            if (future == null) {
                future = ThreadPools.backgroundExecutor.scheduleAtFixedRate(new ScheduledRunnable(), 0, 10, TimeUnit.SECONDS);
            }
        } else {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return "URLResource{" +
            "url=" + url +
            ", lastModified=" + lastModified +
            '}';
    }

    private class ScheduledRunnable implements Runnable {

        @Override
        public void run() {
            if (async) {
                try {
                    URLResource.this.getCachedResource();
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    public static Function<InputStream, Properties> PROPERTIES = inputStream -> {
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return props;
    };

    public static Function<InputStream, Map<String, String>> MAP = inputStream -> {

        Properties props = new LinkedProperties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return props.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()), (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        } ,  LinkedHashMap::new));
    };

    public static <S> Function<InputStream, List<S>> beansFromProperties(Function<String, S> constructor) {
        return
            inputStream -> {
                Map<String, String> properties = MAP.apply(inputStream);
                Map<String, S> result = new LinkedHashMap<>();
                properties.entrySet().forEach(e ->
                    {
                        String[] key = e.getKey().split("\\.", 2);
                        S g = result.get(key[0]);
                        if (g == null) {
                            g = constructor.apply(key[0]);
                            result.put(key[0], g);
                        }
                        if (key.length > 1) {
                            ReflectionUtils.setProperty(g, key[1], e.getValue());
                        }
                    }

                );
                return new ArrayList<>(result.values());
            };

    }
}

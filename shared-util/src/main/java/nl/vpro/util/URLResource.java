package nl.vpro.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 0.37
 */
public class URLResource<T> {


    public static URLResource<Properties> properties(URI url, Consumer<Properties>... callbacks) {
        return new URLResource<>(url, PROPERTIES, new Properties(), callbacks);
    }

    public static URLResource<Map<String, String>> map(URI url, Consumer<Map<String, String>>... callbacks) {
        return new URLResource<>(url, MAP, new HashMap<>(), callbacks);
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
    private boolean async = false;
    private T empty = null;

    private ScheduledFuture<?> future = null;
    private Consumer<T>[] callbacks;



    public URLResource(URI url, Function<InputStream, T> reader, T empty, Consumer<T>... callbacks) {
        this.url = url;
        this.empty = empty;
        this.reader = reader;
        this.callbacks = callbacks;

    }


    public URLResource(URI url, Function<InputStream, T> reader, Consumer<T>... callbacks) {
        this(url, reader, null, callbacks);
    }


    public T get() {
        if (result == null) {
            if (async) {
                return empty;
            }
        }
        if (! async) {
            getCachedResource();
        }

        return result;
    }

    void getCachedResource() {
        if (result != null && lastLoad.plus(minAge).isAfter(Instant.now())) {
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
        } catch (IOException e) {
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
                LOG.info("Loaded {} from {}",  newResult , this.url);
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
            LOG.info("Not loading {} as it is not yet expired", url);
            return;
        }
        boolean httpUrl = connection instanceof HttpURLConnection;
        if (httpUrl && lastModified != null) {
            if (lastLoad == null || lastLoad.isAfter(Instant.now().minus(maxAge))) {
                connection.setRequestProperty("If-Modified-Since", DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified.atOffset(ZoneOffset.UTC)));
            } else {
                LOG.debug("last load was pretty long ago, simply do a normal request");
            }
        }
        if (httpUrl) {
            code = ((HttpURLConnection) connection).getResponseCode();
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
                        LOG.info("Reloaded {}  as it is modified since {}  -> {}", url, prevMod , lastModified);
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
                expires = Instant.now().plus(errorCache);
                LOG.warn(code + ":" +  url + ": (caching until " + expires + ")");


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

    public URLResource<T> setCallbacks(Consumer<T>... callbacks) {
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

        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return props.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
    };
}

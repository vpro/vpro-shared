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
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 0.37
 */
public class URLResource<T> {


    public static URLResource<Properties> properties(URI url) {
        return new URLResource<>(url, PROPERTIES);
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


    private final Function<InputStream, T> reader;

    private T result;

    private long okCount = 0;
    private long notModifiedCount = 0;
    private long notCheckedCount = 0;
    private long changesCount = 0;
    private boolean async = false;

    private ScheduledFuture<?> future = null;


    public URLResource(URI url, Function<InputStream, T> reader) {
        this.url = url;
        this.reader = reader;
    }


    public T get() {
        if (! async || result == null) {
            getCachedResource();
        }
        return result;
    }

    void getCachedResource() {
        if (result != null && lastLoad.plus(minAge).isAfter(Instant.now())) {
            notCheckedCount++;
            return;
        }
        LOG.info("Loading from {}", this.url);
        try {
            if (this.url.getScheme().equals("classpath")) {
                getCachedResource(this.url.toString().substring("classpath:".length() + 1));
            } else {

                URLConnection connection = url.toURL().openConnection();
                getCachedResource(connection);

            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
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
                    lastLoad = Instant.now();
                    lastModified = Instant.now();
                    changesCount++;
                }
            }
            result = newResult;
        } else {
            LOG.warn("Loading from {} resulted null", this.url);
        }
    }

    void getCachedResource(URLConnection connection) throws IOException {
        if (result != null && expires != null && Instant.now().isBefore(expires)) {
            LOG.info("Not loading {} as it is not yet expired", url);
            return;
        }
        boolean ifModifiedCheck = connection instanceof HttpURLConnection;
        if (ifModifiedCheck && lastModified != null) {
            if (lastLoad == null || lastLoad.isAfter(Instant.now().minus(maxAge))) {
                connection.setRequestProperty("If-Modified-Since", DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified.atOffset(ZoneOffset.UTC)));
            } else {
                // last load was pretty long ago, simply do a normal request always.
                ifModifiedCheck = false;
            }
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
                            String[] ma = s.split("\\s*=\\s*", 2);
                            expires = Instant.now().plus(Duration.of(Integer.parseInt(ma[1]), ChronoUnit.SECONDS));
                        }
                    }
                }
                Instant maxExpires = Instant.now().plus(maxAge);
                if (expires != null && expires.isAfter(maxExpires)) {
                    LOG.info("Found expiry {} for {} truncated to {}", expires, url, maxExpires);
                    expires = maxExpires;
                }
                T newResult = reader.apply(stream);
                if (ifModifiedCheck) {
                    if (newResult != null) {
                        if (result == null) {
                            LOG.info("Loaded {} -> {}", url, lastModified);
                        } else {
                            LOG.info("Reloaded {}  as it is modified since {}  -> {}", url, prevMod , lastModified);
                        }
                        changesCount++;
                        result = newResult;
                    }
                } else {
                    if (newResult != null) {
                        if (result != null && !Objects.equals(result, newResult)) {
                            result = newResult;
                            changesCount++;
                            LOG.info("Reloaded {}. It is modified since {} (Reason unknown)", url, lastModified);
                        } else {
                            result = newResult;
                        }
                    }

                }
                stream.close();
                lastLoad = Instant.now();
                break;
            default:
                LOG.error(code + ":" + connection);
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

    public boolean isAsync() {
        return async;
    }

    public URLResource<T> setAsync(boolean async) {
        this.async = async;
        if (this.async && future == null) {
            future = ThreadPools.backgroundExecutor.scheduleAtFixedRate(new ScheduledRunnable(), 0, 10, TimeUnit.SECONDS);
        }
        return this;
    }

    private class ScheduledRunnable implements Runnable {

        @Override
        public void run() {
            if (async) {
                URLResource.this.getCachedResource();
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
}

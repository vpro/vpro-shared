package nl.vpro.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
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

/**
 * A simple http client wrapping exactly one external resource, keeping track of cache headers.
 * @author Michiel Meeuwissen
 * @since 0.37
 */
@Slf4j
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
    private static final int SC_FOUND = 302;
    private static final int SC_MOVED_PERMANENTLY = 301;


    @Getter
    private Instant lastLoad = null;
    @Getter
    private Instant lastTry = null;

    @Getter
    private Integer code = null;
    @Getter
    private URI url;

    @Getter
    private Instant lastModified = null;
    @Getter
    private Instant expires = null;
    @Getter
    private Duration maxAge = Duration.of(1, ChronoUnit.HOURS);
    @Getter
    private Duration minAge = Duration.of(5, ChronoUnit.MINUTES);
    @Getter
    private Duration errorCache = Duration.of(1, ChronoUnit.MINUTES);


    private final Function<InputStream, T> reader;

    private T result;

    @Getter
    private long okCount = 0;
    @Getter
    private long notModifiedCount = 0;
    @Getter
    private long notCheckedCount = 0;
    @Getter
    private long checkedCount = 0;
    @Getter
    private long changesCount = 0;
    @Getter
    private long errorCount = 0;
    @Getter
    private boolean async = false;
    private T empty = null;

    private ScheduledFuture<?> future = null;
    private Consumer<T>[] callbacks;

    @Setter
    @Getter
    private Duration connectTimeout = Duration.ofMillis(1000);
    @Setter
    @Getter
    private Duration readTimeout = Duration.ofMillis(5000);


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
        if (result != null && lastTry != null) {
            Instant dontCheckBefore = lastTry.plus(minAge);
            if (Instant.now().isBefore(dontCheckBefore)) {
                log.debug("Not trying as it is not yet {}", dontCheckBefore);
                notCheckedCount++;
                return;
            }
        }
        checkedCount++;
        log.debug("Loading from {}", this.url);
        if (this.url == null){
            throw new IllegalStateException("URL not set in " + this);
        }
        try {
            if ("classpath".equals(this.url.getScheme())) {
                getCachedResource(this.url.toString().substring("classpath:".length() + 1));
            } else {
                URLConnection connection = openConnection();
                getCachedResource(connection, 0);

            }
        } catch (java.net.UnknownHostException uhe) {
            errorCount++;
            log.warn(uhe.getClass().getName() + " " + uhe.getMessage());
        } catch (IOException e) {
            errorCount++;
            log.error(e.getMessage(), e);
        }
    }

    URLConnection openConnection() throws IOException {
        try {
            return url.toURL().openConnection();
        } catch (IllegalArgumentException ia) {
            log.error("For " + url + " " + ia.getMessage());
            throw ia;
        }
    }

    void getCachedResource(String resource) {
        T newResult = reader.apply(getClass().getClassLoader().getResourceAsStream(resource));
        if (newResult != null) {
            lastLoad = Instant.now();
            lastTry = lastLoad;
            if (result == null) {
                lastModified = lastLoad;
                log.info("Loaded {} from {}", newResult, this.url);
            } else {
                if (!Objects.equals(result, newResult)) {
                    log.info("Reloaded {} from {}", newResult, this.url);
                    lastModified = lastLoad;
                    changesCount++;
                }
            }


            result = newResult;
            callBack();
        } else {
            log.warn("Loading from {} resulted null", this.url);
        }
    }

    void callBack() {
        for (Consumer<T> callback : callbacks) {
            callback.accept(result);
        }
    }

    void getCachedResource(URLConnection connection, int redirectCount) throws IOException {
        if (result != null && expires != null && Instant.now().isBefore(expires)) {
            log.debug("Not loading {} as it is not yet expired", url);
            return;
        }
        boolean httpUrl = connection instanceof HttpURLConnection;
        code = -1;
        if (httpUrl && lastModified != null) {
            Instant alwaysRefreshIfAfter = lastTry.plus(maxAge);
            if (lastTry == null || Instant.now().isBefore(alwaysRefreshIfAfter)) {
                log.debug("last tried at {}, check if modified", lastTry);
                connection.setRequestProperty("If-Modified-Since", DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified.atOffset(ZoneOffset.UTC)));
            } else {
                log.debug("last try at {} was pretty long ago (> {}), simply do a normal request", lastTry, maxAge);
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
                log.warn("For {} (readTimeout: {}, connectTimeout: {}): {}:{}", url, readTimeout, connectTimeout, ste.getClass().getName(), ste.getMessage());
                code = -1;
            } catch (IOException ce) {
                log.error("For " + connection + " " + ce.getMessage());
                throw ce;

            }


        } else {
            code = SC_OK;
        }
        lastTry = Instant.now();
        log.info("Trying {} at {}", url, lastTry);
        switch (code) {
            case SC_NOT_MODIFIED:
                log.debug("Not modified {}", url);
                notModifiedCount++;
                break;
            case SC_OK:
                okCount++;
                log.debug("Loaded {}", url);
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
                                    log.warn("Could not parse {}", s);
                                }
                            } catch (Exception e) {
                                log.warn("Could not parse {}  because {}", s, e.getMessage());
                            }
                        }
                    }
                }
                Instant maxExpires = Instant.now().plus(maxAge);
                if (expires != null && expires.isAfter(maxExpires)) {
                    log.info("Found expiry {} for {} truncated to {}", expires, url, maxExpires);
                    expires = maxExpires;
                }
                T newResult = reader.apply(stream);
                if (newResult != null) {
                    if (result == null) {
                        log.info("Loaded {} -> {}", url, lastModified);
                    } else {
                        log.info("Reloaded {}  as it is modified since {}  -> {}", url, prevMod, lastModified);
                    }
                    changesCount++;
                    result = newResult;
                    callBack();
                }
                stream.close();
                lastLoad = lastTry;
                break;
            case SC_FOUND:
            case SC_MOVED_PERMANENTLY:
                if (redirectCount < 10) {
                    URI redirectTo = URI.create(connection.getHeaderField("Location"));
                    log.info("{} is redirecting to {}", url, redirectTo);
                    if (code == SC_MOVED_PERMANENTLY) {
                        url = redirectTo;
                    }
                    getCachedResource(redirectTo.toURL().openConnection(), redirectCount + 1);
                }
                break;
            default:
                if (result == null) {
                    result = empty;
                }
                lastLoad = lastTry;
                lastModified = null;
                errorCount++;
                expires = Instant.now().plus(errorCache);
                log.warn("{}:{}: (caching until {})", code, url, expires);


        }

    }



    void expire() {
        expires = null;
    }


    public URLResource<T> setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public URLResource<T> setMinAge(Duration minAge) {
        this.minAge = minAge;
        return this;
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
                    log.error(e.getMessage());
                }
            }
        }
    }

    public static Function<InputStream, Properties> PROPERTIES = inputStream -> {
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return props;
    };

    public static Function<InputStream, Map<String, String>> MAP = inputStream -> {

        Properties props = new LinkedProperties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return props
            .entrySet()
            .stream()
            .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()),
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                } ,  LinkedHashMap::new)
            );
    };

    public static <S> Function<InputStream, List<S>> beansFromProperties(Function<String, S> constructor) {
        return
            inputStream -> {
                Map<String, String> properties = MAP.apply(inputStream);
                Map<String, S> result = new LinkedHashMap<>();
                properties.forEach((key1, value) -> {
                    String[] key = key1.split("\\.", 2);
                    S g = result.computeIfAbsent(key[0], k -> constructor.apply(key[0]));
                    if (key.length > 1) {
                        ReflectionUtils.setProperty(g, key[1], value);
                    }
                });
                return new ArrayList<>(result.values());
            };

    }
}

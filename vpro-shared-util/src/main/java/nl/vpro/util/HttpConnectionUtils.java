package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpResponse.BodyHandlers.discarding;

/**
 * @author Michiel Meeuwissen
 * @since 1.74
 */
@Slf4j
public class HttpConnectionUtils {

    public static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> true);

    private HttpConnectionUtils() {

    }

    /**
     * Client used for {@link #getByteSize(String)}}
     */
    private static final HttpClient CLIENT = HttpClient.newBuilder()

        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    /**
     * Executes a HEAD request to determine the bytes size of given URL. For mp3's and such.
     *
     * @since 4.1
     * @return an optional with the size in bytes of the resource represented by the given url.
     */
    public static OptionalLong getOptionalByteSize(String locationUrl) {

        return getOptionalByteSize(locationUrl, (response, exception) -> {
            if (exception != null) {
                log.warn("For {}: {} {}", locationUrl, exception.getClass().getName(), exception.getMessage());
            }
            if (response != null && response.statusCode() != 200) {
                log.warn("HEAD {} returned {}", locationUrl, response.statusCode());
            }
        });
    }

    /**
     * Executes a HEAD request to determine the bytes size of given URL. For mp3's and such.
     *
     * @since 5.8
     * @return an optional with the size in bytes of the resource represented by the given url.
     */
    public static OptionalLong getOptionalByteSize(String locationUrl, BiConsumer<HttpResponse<Void>, Exception> consumer) {
        return headRequest(locationUrl, (response, exception) -> {
            consumer.accept(response, exception);
            if (response != null) {
                if (response.statusCode() == 200) {
                    return response.headers().firstValueAsLong("Content-Length");
                } else {
                    return OptionalLong.empty();
                }
            } else {
               return OptionalLong.empty();
           }
        });
    }




    public static <R> R headRequest(String locationUrl, BiFunction<HttpResponse<Void>, Exception, R> consumer) {
        if (locationUrl == null || ! ENABLED.get()) {
            return consumer.apply(null, null);
        }
        try {
            final URI uri = URI.create(locationUrl);
            final String scheme = uri.getScheme();
            if (!("http".equals(scheme) || "https".equals(scheme))) {
                return consumer.apply(null, null);
            }
            final HttpRequest head = HttpRequest.newBuilder()
                .uri(uri)
                .method("HEAD", noBody())
                .build(); // .HEAD() in java 18
            final HttpResponse<Void> send = CLIENT.send(head, discarding());
            log.debug("HEAD {} returned {}", locationUrl, send.statusCode());
            return consumer.apply(send, null);
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            log.warn("For {}: {} {}", locationUrl, e.getClass().getName(), e.getMessage());
            return consumer.apply(null, e);
        }
    }

    /**
     * Executes a HEAD request to determine the bytes size of given URL. For mp3's and such.
     *
     * @return the size in bytes, or {@code null} if it could not be determined.
     * @see #getOptionalByteSize(String) getOptionalByteSize
     */
    public static Long getByteSize(String u) {
        final OptionalLong result = getOptionalByteSize(u);
        if (result.isPresent()) {
            return result.getAsLong();
        } else {
            return null;
        }
    }
}

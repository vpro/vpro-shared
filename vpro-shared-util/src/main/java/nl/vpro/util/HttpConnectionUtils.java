package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.OptionalLong;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpResponse.BodyHandlers.discarding;

/**
 * @author Michiel Meeuwissen
 * @since 1.74
 */
@Slf4j
public class HttpConnectionUtils {

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
     * @since 4.1
     */
    public static OptionalLong getOptionalByteSize(String locationUrl) {
        URI uri = URI.create(locationUrl);
        String scheme = uri.getScheme();
        if (! ("http".equals(scheme) || "https".equals(scheme))) {
            return OptionalLong.empty();
        }
        final HttpRequest head = HttpRequest.newBuilder()
            .uri(uri)
            .method("HEAD", noBody())
            .build(); // .HEAD() in java 18
        try {
            final HttpResponse<Void> send = CLIENT.send(head, discarding());
            if (send.statusCode() == 200) {
                return send.headers().firstValueAsLong("Content-Length");
            } else {
                log.warn("HEAD {} returned {}", locationUrl, send.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.warn(e.getMessage(), e);
        }
        return OptionalLong.empty();
    }

    /**
     * Executes a HEAD request to determine the bytes size of given URL. For mp3's and such.
     */
    public static Long getByteSize(String u) {
        OptionalLong result = getOptionalByteSize(u);
        if (result.isPresent()) {
            return result.getAsLong();
        } else {
            return null;
        }
    }
}

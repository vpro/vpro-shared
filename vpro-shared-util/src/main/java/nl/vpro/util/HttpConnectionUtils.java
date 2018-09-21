package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Michiel Meeuwissen
 * @since 1.74
 */
@Slf4j
public class HttpConnectionUtils {


    /**
     * TODO in vpro api we find a HttpClient version of this, with connection pooling.
     */
    public static Long getByteSize(String u) {
        try {
            URL url = new URL(u);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("HEAD");
            if (connection.getResponseCode() == 200) {
                String contentLength = connection.getHeaderField("Content-Length");
                if (contentLength != null) {
                    Long result = Long.parseLong(contentLength);
                    log.info("Byte size of {} is {} (determined by head request)", u, result);
                    return result;
                } else {
                    log.warn("No content length in {}" + u);
                    return null;
                }
            } else {
                log.warn("For determining byte sise. Response code {} from {}", connection.getResponseCode(), u);
                return null;
            }
        } catch (MalformedURLException mf) {
            log.debug(mf.getMessage());
            return null;
        } catch (IOException e) {
            log.warn("For determining byte size of {}: {}", u, e.getMessage(), e);
            return null;
        }
    }
}

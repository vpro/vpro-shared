package nl.vpro.rs;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

public class RsUtils {

    public static void contentNegotiation(
        HttpHeaders httpHeaders,
        HttpServletResponse response
    ) {
        String acceptString = httpHeaders.getHeaderString(HttpHeaders.ACCEPT);
        MediaType accept = Arrays.stream(acceptString.split("\\s*,\\s*"))
            .map(MediaType::valueOf)
            .filter(a -> a.isCompatible(APPLICATION_XML_TYPE) || a.isCompatible(APPLICATION_JSON_TYPE))
            .findFirst()
            .orElseThrow(NotAcceptableException::new);
                if (accept.isCompatible(APPLICATION_JSON_TYPE)) {
                    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                } else {
                    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
                }
    }
}

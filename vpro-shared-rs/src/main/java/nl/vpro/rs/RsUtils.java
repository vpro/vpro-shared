package nl.vpro.rs;

import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

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

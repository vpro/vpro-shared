package nl.vpro.rs.converters;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;


/**
 * BasicAuthentication but does not replace existing headers.
 * @author Michiel Meeuwissen
 * @since 2.2
 */
@Slf4j
public class ConditionalBasicAuthentication  implements ClientRequestFilter {

    private final String authHeader;

    public ConditionalBasicAuthentication(String username, String password) {
        this.authHeader = createBasicAuthenticationHeader(username, password);
    }

    public void filter(ClientRequestContext requestContext) {
          List<Object> existing = requestContext.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (existing == null || existing.isEmpty()) {
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, this.authHeader);
        } else {
            log.debug("Request already contains other authorization headers {}", existing);
        }
    }

    private static String createBasicAuthenticationHeader(String username, String password) {
        StringBuffer buf = new StringBuffer(username);
        buf.append(':').append(password);
        return "Basic " + Base64.getEncoder().encodeToString(buf.toString().getBytes(StandardCharsets.UTF_8));
    }
}

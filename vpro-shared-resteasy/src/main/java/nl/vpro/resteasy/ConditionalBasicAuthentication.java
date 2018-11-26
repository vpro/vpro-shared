package nl.vpro.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.util.BasicAuthHelper;

/**
 * BasicAuthentication but does not replace existing headers.
 * @author Michiel Meeuwissen
 * @since 2.2
 */
@Slf4j
public class ConditionalBasicAuthentication  implements ClientRequestFilter {

    private final String authHeader;

    public ConditionalBasicAuthentication(String username, String password) {
        this.authHeader = BasicAuthHelper.createHeader(username, password);
    }

    public void filter(ClientRequestContext requestContext) throws IOException {
          List<Object> existing = requestContext.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (existing == null || existing.isEmpty()) {
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, this.authHeader);
        } else {
            log.debug("Request already contains other authorization headers {}", existing);
        }
    }
}

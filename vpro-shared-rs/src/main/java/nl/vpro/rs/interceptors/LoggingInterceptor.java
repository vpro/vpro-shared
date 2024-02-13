package nl.vpro.rs.interceptors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;

import java.io.*;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;


import nl.vpro.logging.mdc.MDCConstants;

/**
 * @author Michiel Meeuwissen
 * @since 1.79
 */
@Getter
@Provider
@PreMatching
@Slf4j
public class LoggingInterceptor implements ContainerRequestFilter {

    @Setter
    private boolean enabled = true;



    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();

        if ("POST".equals(method) || "PUT".equals(method)) {
            String contentLength = requestContext.getHeaderString("Content-Length");
            if (enabled && (contentLength == null || Long.parseLong(contentLength) < 100000) && ! "PUT".equals(method)) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                IOUtils.copy(requestContext.getEntityStream(), bytes);
                String body = bytes.toString(StandardCharsets.UTF_8);
                int length = Math.min(body.length(), 2000);
                boolean truncated = length < body.length();
                MDC.put(MDCConstants.BODY, "\n" + body.substring(0, length) + (truncated ? "(TRUNCATED, total " + bytes.toByteArray().length + " bytes))" : ""));
                requestContext.setEntityStream(new ByteArrayInputStream(bytes.toByteArray()));
            } else {
                MDC.put(MDCConstants.BODY, "\n" + contentLength + " bytes");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("{}", requestContext.getHeaders());
        }
    }
}

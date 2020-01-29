package nl.vpro.rs.interceptors;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;

/**
 * @author Michiel Meeuwissen
 * @since 1.79
 */
@Provider
@PreMatching
@Slf4j
public class LoggingInterceptor implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();

        if ("POST".equals(method) || "PUT".equals(method)) {
            String contentLength = requestContext.getHeaderString("Content-Length");
            if ((contentLength == null || Long.parseLong(contentLength) < 100000) && ! "PUT".equals(method)) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                IOUtils.copy(requestContext.getEntityStream(), bytes);
                String body = bytes.toString("UTF-8");
                int length = Math.min(body.length(), 2000);
                boolean truncated = length < body.length();
                MDC.put("body", "\n" + body.substring(0, length) + (truncated ? "(TRUNCATED, total " + bytes.toByteArray().length + " bytes))" : ""));
                requestContext.setEntityStream(new ByteArrayInputStream(bytes.toByteArray()));
            } else {
                MDC.put("body", "\n" + contentLength + " bytes");
            }
        }
    }
}

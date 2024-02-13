package nl.vpro.rs.client;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class HeaderInterceptor  implements ReaderInterceptor {

    public static final HeaderInterceptor INSTANCE = new HeaderInterceptor();

    private static final ThreadLocal<MultivaluedMap<String, String>> HEADERS = ThreadLocal.withInitial(() -> null);


    private  HeaderInterceptor() {
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        HEADERS.set(context.getHeaders());
        return context.proceed();
    }

    public static MultivaluedMap<String, String> getHeaders() {
        return HEADERS.get();
    }
}

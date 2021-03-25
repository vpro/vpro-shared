package nl.vpro.rs.client;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

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

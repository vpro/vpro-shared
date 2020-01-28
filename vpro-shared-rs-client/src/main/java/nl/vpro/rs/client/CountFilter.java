package nl.vpro.rs.client;

import java.lang.reflect.Method;

import javax.ws.rs.client.*;

import org.slf4j.Logger;

import nl.vpro.jmx.CountAspect;

/**
 * @author Michiel Meeuwissen
 * @since 1.65
 */
public class CountFilter implements ClientResponseFilter  {

    private final Logger log;

    public CountFilter(Logger log) {
        this.log = log;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();

        if (local != null) {
            String key = methodToString(local.getMethod());
            if (responseContext.getStatus() != 200) {
                key += "/" + responseContext.getStatus();
            }
            String cached = (String) requestContext.getProperty("cached");
            if (cached != null) {
                key += "/" + cached;
            }
            local.requestEnd(requestContext.getUri(), key);
        } else {
            log.warn("No count aspect local found for {}", requestContext.getUri());
        }


    }

     static String methodToString(Method m) {
         return m.getDeclaringClass().getSimpleName() + "." + m.getName();
    }

}

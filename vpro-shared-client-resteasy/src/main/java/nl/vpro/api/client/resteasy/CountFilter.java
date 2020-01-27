package nl.vpro.api.client.resteasy;

import javax.ws.rs.client.*;

import org.slf4j.Logger;

import nl.vpro.jmx.CountAspect;

/**
 * @author Michiel Meeuwissen
 * @since 1.65
 */
class CountFilter implements ClientResponseFilter  {

    private final Logger log;

    CountFilter(Logger log) {
        this.log = log;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();

        if (local != null) {
            String key = AbstractApiClient.methodToString(local.getMethod());
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
}

package nl.vpro.api.client.resteasy;

import java.io.IOException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.slf4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @since 1.65
 */
class CountFilter implements ClientResponseFilter  {

    private final ObjectName name;
    private final Logger log;



    CountFilter(ObjectName name, Logger log) {
        this.name = name;
        this.log = log;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();

        if (local != null) {
            String key = AbstractApiClient.methodToString(local.method);
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

    ObjectName getObjectName(String m) {
        try {
            return new ObjectName(name.toString() + ",name=" + m.replaceAll(":", "_"));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }
}

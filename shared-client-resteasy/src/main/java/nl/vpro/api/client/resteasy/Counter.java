package nl.vpro.api.client.resteasy;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

/**
 * @author Michiel Meeuwissen
 * @since 1.57
 */
public class Counter extends AtomicLong implements CounterMBean {


    public Counter(ObjectName name) {
        super(0L);
        AbstractApiClient.registerBean(name, this);
    }


    @Override
    public long getCount() {
        return get();

    }
}

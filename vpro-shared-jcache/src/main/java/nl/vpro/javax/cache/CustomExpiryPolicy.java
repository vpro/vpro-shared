package nl.vpro.javax.cache;

import java.util.concurrent.TimeUnit;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

public class CustomExpiryPolicy implements ExpiryPolicy {

    private java.time.Duration expiry = java.time.Duration.ofMinutes(5);
    @Override
    public Duration getExpiryForCreation() {
        return new Duration(TimeUnit.MILLISECONDS, expiry.toMillis());
    }

    @Override
    public Duration getExpiryForAccess() {
        return new Duration(TimeUnit.MILLISECONDS, expiry.toMillis());
    }

    @Override
    public Duration getExpiryForUpdate() {
        return new Duration(TimeUnit.MILLISECONDS, expiry.toMillis());
    }
}

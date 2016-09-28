package nl.vpro.api.client.resteasy;

import java.time.Duration;

/**
 * @author Michiel Meeuwissen
 * @since 0.51
 */
public interface AbstractApiClientMBean {
    Duration getConnectionRequestTimeout();
    void setConnectionRequestTimeout(Duration connectionRequestTimeout);
    Duration getConnectTimeout();
    void setConnectTimeout(Duration connectTimeout);
    Duration getSocketTimeout();
    void setSocketTimeout(Duration socketTimeout);

}

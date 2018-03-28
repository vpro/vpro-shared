package nl.vpro.api.client.resteasy;

import javax.management.MXBean;

import nl.vpro.jmx.Units;

/**
 * @author Michiel Meeuwissen
 * @since 0.51
 */
@MXBean
public interface AbstractApiClientMXBean {


    String getBaseUrl();
    void setBaseUrl(String url);

    @Units("duration")
    String getConnectionRequestTimeout();
    void setConnectionRequestTimeout(String connectionRequestTimeout);
    String getConnectTimeout();
    void setConnectTimeout(String connectTimeout);
    String getSocketTimeout();
    void setSocketTimeout(String socketTimeout);
    String getCounts();
    long getCount(String method);
    long getTotalCount();

    String getCountWindowString();
    void setCountWindowString(String s);
    Integer getBucketCount();
    void setBucketCount(Integer bucketCount);

    String getWarnThresholdString();
    void setWarnThresholdString(String s);

    Integer getMaxConnections();
    void setMaxConnections(Integer maxConnections);
    Integer getMaxConnectionsPerRoute();
    void setMaxConnectionsPerRoute(Integer maxConnectionsPerRoute);

    Integer getMaxConnectionsNoTimeout();
    void setMaxConnectionsNoTimeout(Integer maxConnections);
    Integer getMaxConnectionsPerRouteNoTimeout();
    void setMaxConnectionsPerRouteNoTimeout(Integer maxConnectionsPerRoute);


    @Units("events/minute")
    double getRate();

    String getInitializationInstant();

    String test(String arg);

}

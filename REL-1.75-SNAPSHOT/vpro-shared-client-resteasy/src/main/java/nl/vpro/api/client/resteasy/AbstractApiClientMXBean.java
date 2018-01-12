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

    int getMaxConnections();
    void setMaxConnections(int maxConnections);
    int getMaxConnectionsPerRoute();
    void setMaxConnectionsPerRoute(int maxConnectionsPerRoute);

    int getMaxConnectionsNoTimeout();
    void setMaxConnectionsNoTimeout(int maxConnections);
    int getMaxConnectionsPerRouteNoTimeout();
    void setMaxConnectionsPerRouteNoTimeout(int maxConnectionsPerRoute);


    @Units("events/minute")
    double getRate();

    String getInitializationInstant();

}

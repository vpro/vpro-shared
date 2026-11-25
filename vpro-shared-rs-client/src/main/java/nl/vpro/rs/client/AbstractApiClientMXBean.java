package nl.vpro.rs.client;

import javax.management.MXBean;

import nl.vpro.jmx.Description;
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

    @Units("duration")
    String getConnectTimeout();
    void setConnectTimeout(String connectTimeout);

    @Units("duration")
    String getSocketTimeout();
    void setSocketTimeout(String socketTimeout);

    /**
     * The socket (read) timeout for the 'no timeout' clients, which are meant for long-running (streaming) calls. Defaults to {@code null}, meaning no read timeout at all.
     * Note that this is a maximum time of inactivity (no bytes received), not a limit on the total duration of a call.
     * @since 6.0
     */
    @Units("duration")
    String getSocketTimeoutNoTimeout();
    void setSocketTimeoutNoTimeout(String socketTimeoutNoTimeout);

    String getConnectionInPoolTTL();
    void  setConnectionInPoolTTL(String connectionInPoolTTL);

    String getValidateAfterInactivity();

    void  setValidateAfterInactivity(String validateAfterInactivity);

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

    void invalidate();


    @Description("Just performs a test call. By this one check via jmx whether the client is working. The argument is probably some id of the items in the api this client is serving")
    String test(String arg);

}

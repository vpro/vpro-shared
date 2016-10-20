package nl.vpro.api.client.resteasy;

/**
 * @author Michiel Meeuwissen
 * @since 0.51
 */
public interface AbstractApiClientMBean {
    String getConnectionRequestTimeout();
    void setConnectionRequestTimeout(String connectionRequestTimeout);
    String getConnectTimeout();
    void setConnectTimeout(String connectTimeout);
    String getSocketTimeout();
    void setSocketTimeout(String socketTimeout);

}

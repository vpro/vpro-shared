package nl.vpro.elasticsearchclient;

import javax.management.MXBean;

/**
 * @author Michiel Meeuwissen
 * @since 2.10
 */
@MXBean
public interface ClientElasticSearchFactoryMXBean {


    String getClusterName();

    void setClusterName(String clusterName);

    String getUnicastHosts();

    void setUnicastHosts(String unicastHosts);

    String invalidate();

    String getClients();
}

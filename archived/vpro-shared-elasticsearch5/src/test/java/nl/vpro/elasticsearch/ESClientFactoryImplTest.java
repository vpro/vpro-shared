package nl.vpro.elasticsearch;

import java.net.InetAddress;
import java.util.Arrays;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.util.UrlProvider;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Ignore("Work in progress")
public class ESClientFactoryImplTest {

    @Test
    public void testSetTransportAddresses() throws Exception {
        TransportClientFactory factory = new TransportClientFactory();
        factory.setTransportAddresses(new UrlProvider("a", 100), new UrlProvider("b", 101));
        TransportClient client = (TransportClient) factory.buildClient();
        assertEquals(Arrays.asList(new InetSocketTransportAddress(InetAddress.getByName("a"), 100), new InetSocketTransportAddress(InetAddress.getByName("b"), 101)), client.transportAddresses());
    }

    @Test
    public void testGetClusterName() {
        TransportClientFactory factory = new TransportClientFactory();
        factory.setClusterName("vpro");

        TransportClient client = (TransportClient) factory.buildClient();
        assertEquals("vpro", client.settings().get("cluster.name"));
    }

    @Test
    public void testSetSniffCluster() {
        TransportClientFactory factory = new TransportClientFactory();
        //factory.setSniffCluster(false);

        TransportClient client = (TransportClient) factory.buildClient();
        assertEquals("" + client.settings().getAsMap(), "false", client.settings().get("client.transport.sniff"));
    }

    @Test
    public void testSetIgnoreClusterName() {
        TransportClientFactory factory = new TransportClientFactory();
        //factory.setIgnoreClusterName(true);

        TransportClient client = (TransportClient) factory.buildClient();
        assertEquals("true", client.settings().get("client.transport.ignore_cluster_name"));
    }

    @Test
    public void testSetPingTimeoutInSeconds() {
        TransportClientFactory factory = new TransportClientFactory();
        //factory.setPingTimeoutInSeconds(10);

        TransportClient client = (TransportClient) factory.buildClient();
        assertEquals("" + client.settings().getAsMap(), "10s", client.settings().get("client.transport.ping_timeout"));
    }

    @Test
    public void testSetPingIntervalInSeconds() {
        TransportClientFactory factory = new TransportClientFactory();
        //factory.setPingIntervalInSeconds(100);

        TransportClient client = (TransportClient) factory.buildClient();
        assertEquals("" + client.settings().getAsMap(), "100s", client.settings().get("client.transport.nodes_sampler_interval"));
    }
}

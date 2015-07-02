package nl.vpro.elasticsearch;

import java.util.Arrays;

import org.elasticsearch.client.Client;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.util.UrlProvider;


@Ignore("required running es")
public class JoinClusterClientFactoryTest {

    int port = 9302;
    String clusterName = "elasticsearch_acceptatie";

    @Test
    public void join() {
        JoinClusterClientFactory factory = new JoinClusterClientFactory();
        factory.setClusterName(clusterName);
        factory.setUnicastHosts("localhost[" + port + "]");
        Client client = factory.buildClient("test");
        System.out.println(client.prepareCount("test").get().getCount());
    }


    @Test
    public void join2() {
        ESClientFactoryImpl factory = new ESClientFactoryImpl();
        factory.setClusterName(clusterName);
        factory.setTransportAddresses(Arrays.asList(new UrlProvider("localhost", port)));
        factory.setSniffCluster(true);
        factory.setIgnoreClusterName(false);


        Client client = factory.buildClient("test");

        System.out.println(client.prepareCount("test").get().getCount());
    }
}

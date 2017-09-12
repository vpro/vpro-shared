package nl.vpro.elasticsearch;

import org.elasticsearch.client.Client;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.util.UrlProvider;


@Ignore("required running es")
public class JoinClusterClientFactoryTest {

    int port = 9202;
    //String clusterName = "elasticsearch_acceptatie";
    //String clusterName = "poms10aas";
    String clusterName = "elasticsearch";

    @Test
    public void joinUnicast() {
        JoinClusterClientFactory factory = new JoinClusterClientFactory();
        factory.setClusterName(clusterName);
        factory.setUnicastHosts("localhost[" + port + "]");
        Client client = factory.client("media");
        //System.out.println(client.prepareCount("test").get().getCount());
    }


    @Test
    public void joinMulticast() {
        JoinClusterClientFactory factory = new JoinClusterClientFactory();
        factory.setClusterName(clusterName);
        factory.setUnicastHosts("");
        Client client = factory.client("media");
        //System.out.println(client.prepareCount("media").get().getCount());
    }


    @Test
    public void join2() {
        TransportClientFactory factory = new TransportClientFactory();
        factory.setClusterName(clusterName);
        factory.setTransportAddresses(new UrlProvider("localhost", port));

        Client client = factory.client("test");

        //System.out.println(client.prepareCount("test").get().getCount());
    }


    @Test
    public void join3() {
        TransportClientFactory factory = new TransportClientFactory();
        factory.setTransportAddresses(new UrlProvider("localhost", port));

        Client client = factory.client("test");
    }
}

package nl.vpro.elasticsearch7;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.junit.jupiter.api.Test;

import nl.vpro.util.UrlProvider;


@Slf4j
public class TransportClientFactoryITest {

    int port = 9300;
    //String clusterName = "elasticsearch_acceptatie";
    //String clusterName = "poms10aas";
    String clusterName = System.getProperty("integ.cluster.name", "elasticsearch");



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

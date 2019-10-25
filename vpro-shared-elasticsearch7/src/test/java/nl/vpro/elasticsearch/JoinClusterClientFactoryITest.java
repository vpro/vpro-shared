package nl.vpro.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

import org.elasticsearch.client.Client;
import org.junit.Ignore;
import org.junit.Test;


@Slf4j
public class JoinClusterClientFactoryITest {


    String clientName = JoinClusterClientFactoryITest.class.getName();
    int port = 9300;
    //String clusterName = "elasticsearch_acceptatie";
    //String clusterName = "poms10aas";
    String clusterName = System.getProperty("integ.cluster.name", "elasticsearch");

    @SuppressWarnings("deprecation")
    @Test
    public void joinUnicast() throws ExecutionException, InterruptedException {
        JoinClusterClientFactory factory = new JoinClusterClientFactory();
        factory.setClusterName(clusterName);
        factory.setUnicastHosts("localhost:" + port);
        factory.setNodeName("Test-" + System.currentTimeMillis());
        Client client = factory.client(clientName);
        log.info("{}", client.prepareSearch().get().getHits().getTotalHits());
        log.info("{}", client.admin().cluster().prepareClusterStats().execute().get());
    }


    @SuppressWarnings("deprecation")
    @Test
    @Ignore("Doesn't work?")
    public void joinMulticast() throws ExecutionException, InterruptedException {
        JoinClusterClientFactory factory = new JoinClusterClientFactory();
        factory.setClusterName(clusterName);
        factory.setUnicastHosts("");
        factory.setNodeName("Test-" + System.currentTimeMillis());
        Client client = factory.client(clientName);
        log.info("{}", client.prepareSearch().get().getHits().getTotalHits());
        log.info("{}", client.admin().cluster().prepareClusterStats().execute().get());
        //System.out.println(client.prepareCount("media").get().getCount());
    }


}

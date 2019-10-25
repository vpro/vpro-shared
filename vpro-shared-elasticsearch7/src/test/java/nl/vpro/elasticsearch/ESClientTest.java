package nl.vpro.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Test;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ESClientTest {


    @Test
    public void test() throws UnknownHostException {
        Settings settings = Settings.builder()
            //.put("cluster.name", "elasticsearch")
            .build();
        TransportClient client = new PreBuiltTransportClient(settings)
            .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300))
        ;
        log.info("Built {}", client);
        log.info("" + client.connectedNodes());
    }
}

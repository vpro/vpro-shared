package nl.vpro.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PreDestroy;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import nl.vpro.util.UrlProvider;

/**
 */
@Slf4j
public class TransportClientFactory implements  ESClientFactory {

    private List<UrlProvider> transportAddresses = Collections.emptyList();


    private String clusterName;


    private boolean implicitHttpToJavaPort = false;

    private Client client = null;

    Client buildClient() {
        return client("test");
    }

    @Override
    public Client client(String logName){
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    log.info("Constructing client on behalf of {}", logName);
                    try {
                        client = constructClient(logName);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    log.info("Construction client on behalf of {} not needed (already happend in other thread)", logName);
                }
            }
        }
        return client;


    }

    private Client constructClient(String logName) throws UnknownHostException {
        Settings.Builder builder = Settings.builder();
        TransportClient transportClient = new PreBuiltTransportClient(builder.build());
        for (UrlProvider urlProvider : transportAddresses) {
            int port = urlProvider.getPort();
            if (implicitHttpToJavaPort && port < 9300 && port >= 9200) {
                log.info("Port is configured {}, but we need a java protocol port. Taking {}", port, port + 100);
                port += 100;
            }
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(urlProvider.getHost()), port));
        }
        log.info("Build es client {} {} ({})", logName, transportAddresses, clusterName);

        return transportClient;
    }



    public void setTransportAddresses(UrlProvider... transportAddresses) {
        reset();
        this.transportAddresses = Arrays.asList(transportAddresses);
    }


    public void setClusterName(String clusterName) {
        reset();
        this.clusterName = clusterName;
    }



    public void setImplicitHttpToJavaPort(boolean implicitHttpToJavaPort) {
        this.implicitHttpToJavaPort = implicitHttpToJavaPort;
    }

    private void reset() {
        shutdown();
        client = null;
    }
    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }
    @Override
    public String toString() {
        return transportAddresses + " " + clusterName;
    }
}

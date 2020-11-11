package nl.vpro.elasticsearch.highlevel;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import nl.vpro.elasticsearchclient.ESClientBuilderFactory;

/**
 */
@Slf4j
public class HighLevelClientFactory  {

    private final ESClientBuilderFactory lowLevelFactory;

    private RestHighLevelClient client = null;

    @Inject
    public HighLevelClientFactory(ESClientBuilderFactory lowLevelFactory) {
        this.lowLevelFactory = lowLevelFactory;
    }

    RestHighLevelClient buildClient() {
        return client("test");
    }


    public RestHighLevelClient client(String logName){
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = constructClient(logName);
                } else {
                    log.debug("Construction client on behalf of {} not needed (already happend in other thread)", logName);
                }
            }
        }
        return client;


    }

    private RestHighLevelClient constructClient(String logName) {
        RestClientBuilder builder = lowLevelFactory.getClientBuilder();
        return new RestHighLevelClient(builder);
    }

}

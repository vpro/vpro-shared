package nl.vpro.elasticsearch.highlevel;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import javax.inject.Inject;

import org.elasticsearch.client.*;

import nl.vpro.elasticsearchclient.ESClientBuilderFactory;
import nl.vpro.elasticsearchclient.ESClientFactory;

/**
 */
@Slf4j
public class HighLevelClientFactory  implements ESClientFactory  {

    private final ESClientBuilderFactory lowLevelFactory;

    private RestHighLevelClient client = null;

    @Inject
    public HighLevelClientFactory(ESClientBuilderFactory lowLevelFactory) {
        this.lowLevelFactory = lowLevelFactory;
    }

    @Override
    public RestClient client(String logName, Consumer<RestClient> callback) {
        return highLevelClient(logName).getLowLevelClient();
    }

    RestHighLevelClient buildClient() {
        return highLevelClient("test");
    }


    public RestHighLevelClient highLevelClient(String logName){
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

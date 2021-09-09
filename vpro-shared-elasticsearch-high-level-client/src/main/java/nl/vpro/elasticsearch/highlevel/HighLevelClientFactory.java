package nl.vpro.elasticsearch.highlevel;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

import javax.inject.Inject;

import org.elasticsearch.client.*;

import nl.vpro.elasticsearchclient.*;

/**
 */
@Slf4j
public class HighLevelClientFactory  implements ESClientFactory  {

    @Getter
    private final ClientElasticSearchFactory lowLevelFactory;

    private RestHighLevelClient client = null;

    @Inject
    public HighLevelClientFactory(ClientElasticSearchFactory lowLevelFactory) {
        this.lowLevelFactory = lowLevelFactory;
    }

    @Override
    public RestClient client(String logName, Consumer<RestClient> callback) {
        return highLevelClient(logName).getLowLevelClient();
    }

    public RestHighLevelClient highLevelClient() {
        return highLevelClient(null);
    }

    public synchronized RestHighLevelClient highLevelClient(String logName){
        if (client == null) {
            client = constructClient(logName);
        }
        return client;
    }

    private RestHighLevelClient constructClient(String logName) {
        RestClientBuilder builder = lowLevelFactory.getClientBuilder();
        if (builder == null) {
            throw new IllegalStateException();
        }
        return new RestHighLevelClient(builder);
    }

}

package nl.vpro.elasticsearchclient;

import java.util.function.Consumer;

import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.slf4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
public abstract class AbstractResponseListener implements ResponseListener {

    private final Logger log;
    private final Consumer<String> callback;

    public AbstractResponseListener(Logger log, Consumer<String> callback) {
        this.log = log;
        this.callback = callback;
    }

    /**
     * Method invoked if the request yielded a successful response
     */
    @Override
    public void onSuccess(Response response) {
        try {
            String s = handleResponse(response);
            callback.accept(s);
        } catch (Throwable e) {
            callback.accept(e.getMessage());
        }

    }



    protected abstract String handleResponse(Response t);

    @Override
    public void onFailure(Exception e) {
        log.error(e.getMessage(), e);
        callback.accept(e.getMessage());
    }

}

package nl.vpro.elasticsearchclient;


import lombok.Lombok;

import java.net.ConnectException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.elasticsearch.client.RestClient;

/**
 * @author Michiel Meeuwissen
 * @since 1.75
 */
public interface AsyncESClientFactory extends ESClientFactory {

    /**
     * Returns a client. The client may be cached by the factory.
     */

    @Override
    default RestClient client(String logName, Consumer<RestClient> callback) {
        try {
            return clientAsync(logName, callback).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Lombok.sneakyThrow(e);
        }  catch (ExecutionException e) {
            if (e.getCause() instanceof ConnectException) {
                invalidate();
            }
            throw Lombok.sneakyThrow(e);
        }
    }

    /**
     * Returns client, after that checks (e.g. on clustername) are performed asynchronously.
     */
    Future<RestClient> clientAsync(String logName, Consumer<RestClient> callback);


}

package nl.vpro.elasticsearch;

import org.elasticsearch.client.Client;

/**
 * @author Michiel Meeuwissen
 * @since 3.6
 */
public interface ESClientFactory {

    Client buildClient(String logName);
}

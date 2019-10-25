package nl.vpro.elasticsearch;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;

import javax.annotation.PostConstruct;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
@Slf4j
public class ClientFactorySwitcher implements ESClientFactory, ClientFactorySwitcherMBean  {

    private final Map<String, ESClientFactory> map = new HashMap<>();

    private String configured;

    private String name = ClientFactorySwitcher.class.getName();

    @Setter
    private boolean testAfterConstruct = false;

    public ClientFactorySwitcher(String configured, String name, Map<String, ESClientFactory> map) {
        this.name = name;
        this.map.putAll(map);
        setConfigured(configured);
    }

    @PostConstruct
    public void init() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName oname = new ObjectName("nl.vpro.elasticsearch:name=" + name);
        mbs.registerMBean(this, oname);

        if (testAfterConstruct) {
            try {
                log.info("Using {}", map.get(configured));
                Client client = client("afterconstruct");
                long count = client.prepareSearch().execute().get().getHits().getTotalHits().value;
                client.close();
                log.info("Found {} objects in {}", count, this);

            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public Client client(String logName) {
        return map.get(configured).client(logName);
    }


    @Override
    public void setConfigured(String configured) {
        if (! map.containsKey(configured)) {
            throw new IllegalArgumentException("Configured should be one of " + map.keySet());
        }
        this.configured = configured;

    }
    @Override
    public String getConfigured() {
        return configured;
    }

    @Override
    public String toString() {
        return String.valueOf(map) + " (" + configured + ")";
    }

}

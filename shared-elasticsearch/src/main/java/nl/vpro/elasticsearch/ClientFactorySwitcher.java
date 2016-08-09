package nl.vpro.elasticsearch;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.management.*;

import org.elasticsearch.client.Client;

/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
public class ClientFactorySwitcher implements ESClientFactory, ClientFactorySwitcherMBean  {

    private final Map<String, ESClientFactory> map = new HashMap<>();

    private String configured;

    private String name = ClientFactorySwitcher.class.getName();

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

}

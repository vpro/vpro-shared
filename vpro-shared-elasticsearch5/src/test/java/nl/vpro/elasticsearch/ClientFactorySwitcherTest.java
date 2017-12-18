package nl.vpro.elasticsearch;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.util.UrlProvider;

/**
 * @author Michiel Meeuwissen
 * @since 0.48
 */
@Ignore
public class ClientFactorySwitcherTest {


    @Test
    public void test() {
        Map<String, ESClientFactory> map = new HashMap<>();
        TransportClientFactory impl = new TransportClientFactory();
        //impl.setIgnoreClusterName(true);
        impl.setTransportAddresses(UrlProvider.fromUrl("http://localhost:9205"));
        impl.setImplicitHttpToJavaPort(true);
        map.put("direct", impl);

        ClientFactorySwitcher switcher = new ClientFactorySwitcher("direct", "name", map);
        //System.out.println(switcher.client("test").prepareCount("apimedia").get().getCount());
    }

    @Test
    public void anothertest() {

    }

}

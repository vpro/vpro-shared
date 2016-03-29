package nl.vpro.hibernate;

import nl.vpro.test.util.jackson2.Jackson2TestUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
public class JsonBridgeTest {

    @Test
    public void testObjectToString() throws Exception {
        String string = "[{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/320x180_180.m4v\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/640x360_1000.m4v\"},{\"programUrl\":\"http://content.omroep.nl/avrotros/transcoding/communicatie/1422371188061/854x480_1500.m4v\"}]\n" 
            ;

        JsonBridge bridge = new JsonBridge();
        Map<String, String> params = new HashMap<>();
        params.put("class", Location[].class.getName());

        bridge.setParameterValues(params);

        Location[] o = (Location[]) bridge.stringToObject(string);
        

        Jackson2TestUtil.roundTripAndSimilar(o, string);


    }
}

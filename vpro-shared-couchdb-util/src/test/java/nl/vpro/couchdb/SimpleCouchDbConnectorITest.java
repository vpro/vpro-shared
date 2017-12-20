package nl.vpro.couchdb;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.jackson2.Jackson2Mapper;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.73
 */
@Ignore("docs.poms.omroep.nl does not exist any more")
public class SimpleCouchDbConnectorITest {

    @Test
    public void get() throws Exception {

        SimpleCouchDbConnector connector = SimpleCouchDbConnector.builder()
            .host("docs.poms.omroep.nl")
            .path("media")
            .build();

        Map mo = Jackson2Mapper.getLenientInstance().readValue(connector.get("POW_03603111"), Map.class);
        assertThat(mo.get("mid")).isEqualTo("POW_03603111");
    }

}

package nl.vpro.couchdb;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import nl.vpro.jackson2.Jackson2Mapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 1.73
 */
@Slf4j
public class SimpleCouchDbConnectorITest {

    @Test
    public void get() throws Exception {

        SimpleCouchDbConnector connector = SimpleCouchDbConnector.builder()
            .host("docs.poms.omroep.nl")
            .path("media")
            .build();

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        IOUtils.copy(connector.get("POW_03603111"), result);
        log.info("{}", new String(result.toByteArray()));
        Map mo = Jackson2Mapper.getLenientInstance().readValue(result.toByteArray(), Map.class);
        assertThat(mo.get("mid")).isEqualTo("POW_03603111");
    }

}

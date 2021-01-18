package nl.vpro.elasticsearchclient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import nl.vpro.jackson2.Jackson2Mapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class IndexHelperTest {

    @Test
    public void handleResponse() throws IOException {
        String logEntry = IndexHelper.handleResponse(Jackson2Mapper.getLenientInstance().readTree("{\"_index\":\"apimedia-0\",\"_type\":\"_doc\",\"_id\":\"BINDINC_80586773\",\"status\":400,\"error\":{\"type\":\"strict_dynamic_mapping_exception\",\"reason\":\"mapping set to strict, dynamic introduction of [audioAttributes] within [scheduleEvents.avAttributes] is not allowed\"}}".getBytes(StandardCharsets.UTF_8)), "foo", "123");
        assertThat(logEntry).isEqualTo("status:400 {\"type\":\"strict_dynamic_mapping_exception\",\"reason\":\"mapping set to strict, dynamic introduction of [audioAttributes] within [scheduleEvents.avAttributes] is not allowed\"}");
    }
}

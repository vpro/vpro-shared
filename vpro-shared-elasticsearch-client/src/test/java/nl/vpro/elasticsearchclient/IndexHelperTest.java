package nl.vpro.elasticsearchclient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import nl.vpro.jackson3.Jackson3Mapper;
import nl.vpro.util.TimeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class IndexHelperTest {

    @Test
    public void handleResponse() {
        String logEntry = IndexHelper.handleResponse(Jackson3Mapper.getLenientInstance().reader().readTree("{\"_index\":\"apimedia-0\",\"_type\":\"_doc\",\"_id\":\"BINDINC_80586773\",\"status\":400,\"error\":{\"type\":\"strict_dynamic_mapping_exception\",\"reason\":\"mapping set to strict, dynamic introduction of [audioAttributes] within [scheduleEvents.avAttributes] is not allowed\"}}".getBytes(StandardCharsets.UTF_8)), "foo", "123");
        assertThat(logEntry).isEqualTo("status:400 {\"type\":\"strict_dynamic_mapping_exception\",\"reason\":\"mapping set to strict, dynamic introduction of [audioAttributes] within [scheduleEvents.avAttributes] is not allowed\"}");
    }

    @Test
    public void refreshInterval() {
        String settings = """
            {"routing":{"allocation":{"include":{"_tier_preference":"data_content"}}},"refresh_interval":"-1","number_of_shards":"2","provided_name":"pageupdates-0","creation_date":"1699902855760","number_of_replicas":"0","uuid":"IQl8c50QRjSzgmhTl3jYYQ","version":{"created":"7140199"}}
            """;
        Duration duration = IndexHelper.getRefreshInterval(Jackson3Mapper.getInstance().reader().readTree(settings));
        assertThat(duration).isEqualTo(TimeUtils.MAX_DURATION);
    }

    @Test
    public void refreshInterval2() {
        String settings = """
            {"routing":{"allocation":{"include":{"_tier_preference":"data_content"}}},"refresh_interval":"31s","number_of_shards":"2","provided_name":"pageupdates-0","creation_date":"1699902855760","number_of_replicas":"0","uuid":"IQl8c50QRjSzgmhTl3jYYQ","version":{"created":"7140199"}}
            """;
        Duration duration = IndexHelper.getRefreshInterval(Jackson3Mapper.getInstance().reader().readTree(settings));
        assertThat(duration).isEqualTo(Duration.ofSeconds(31));
    }
}





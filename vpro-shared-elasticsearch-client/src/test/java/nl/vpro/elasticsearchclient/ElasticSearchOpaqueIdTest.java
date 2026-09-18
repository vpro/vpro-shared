package nl.vpro.elasticsearchclient;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchOpaqueIdTest {

    @AfterEach
    void clearMdc() {
        ThreadContext.clearAll();
    }

    @Test
    void addsTheRequestMethodAndPathWithoutQueryParameters() {
        ThreadContext.put("request", "GET /v1/api/media/POW_01050844?apiKey=secret");

        assertThat(ElasticSearchOpaqueId.withRequest("api-media"))
            .isEqualTo("api-media GET /v1/api/media/POW_01050844");
    }

    @Test
    void preservesTheOperationWhenNoRequestIsAvailable() {
        assertThat(ElasticSearchOpaqueId.withRequest("api-media-redirect-cache"))
            .isEqualTo("api-media-redirect-cache");
    }
}

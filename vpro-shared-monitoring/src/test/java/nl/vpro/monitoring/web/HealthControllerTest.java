package nl.vpro.monitoring.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import nl.vpro.monitoring.domain.Health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration("/manage-servlet.xml")
class HealthControllerTest {
    @Autowired
    private HealthController healthController;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void initialValues() {
        // Pragmatic testing of initial values. Using #wac and #mockMvc is more or less impossible.
        ResponseEntity<Health> response = new HealthController().health();
        assertThat(response.getStatusCodeValue()).isEqualTo(503);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getMessage()).isEqualTo("Application starting");
    }

    @Test
    void statusReady() throws Exception {
        healthController.onApplicationEvent((ContextRefreshedEvent) null);

        mockMvc.perform(
            get("/health")
                .accept(APPLICATION_JSON_VALUE)
        ).andExpect(status().is(200))
            .andExpect(jsonPath("$.status", is(200)))
            .andExpect(jsonPath("$.message", is("Application ready")));
    }

    @Test
    void statusStopping() throws Exception {
        healthController.onApplicationEvent((ContextStoppedEvent) null);

        mockMvc.perform(
            get("/health")
                .accept(APPLICATION_JSON_VALUE)
        ).andExpect(status().is(503))
            .andExpect(jsonPath("$.status", is(503)))
            .andExpect(jsonPath("$.message", is("Application shutdown")));
    }
}

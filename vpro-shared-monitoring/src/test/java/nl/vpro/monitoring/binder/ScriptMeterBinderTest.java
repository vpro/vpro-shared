package nl.vpro.monitoring.binder;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.vpro.util.CommandExecutor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Log4j2
class ScriptMeterBinderTest {


    CommandExecutor commandExecutor = mock(CommandExecutor.class);

    @BeforeEach
    public void setup() {
        when(commandExecutor.lines(anyString(), anyString())).thenReturn(Stream.of(
            "user_api	42	api=pages,user=Aegosei0(ntr)",
            "user_api	58	api=pages,user=ione7ahfij(vpronl)",
            "users	58	user=ione7ahfij(vpronl)",
            "users	42	user=Aegosei0(ntr)",
            "methods	86	method=POST,api=pages",
            "methods	14	method=GET,api=pages",
            "api	100	api=pages"
        ));
    }

    @Test
    public void test() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ScriptMeterBinder binder = new ScriptMeterBinder("test", commandExecutor, "/data/logs", "NATTY:1 week before now");
        binder.bindTo(registry);
        binder.run();
        assertThat(registry.getMetersAsString()).isEqualTo("""
            api(GAUGE)[api='pages']; value=100.0 events
            methods(GAUGE)[api='pages', method='GET']; value=14.0 events
            methods(GAUGE)[api='pages', method='POST']; value=86.0 events
            user_api(GAUGE)[api='pages', user='ione7ahfij(vpronl)']; value=58.0 events
            user_api(GAUGE)[api='pages', user='Aegosei0(ntr)']; value=42.0 events
            users(GAUGE)[user='Aegosei0(ntr)']; value=42.0 events
            users(GAUGE)[user='ione7ahfij(vpronl)']; value=58.0 events""");
    }



}

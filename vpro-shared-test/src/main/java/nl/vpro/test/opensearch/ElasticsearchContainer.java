package nl.vpro.test.opensearch;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.apache.http.HttpHost;
import org.testcontainers.containers.GenericContainer;

@Slf4j
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {
    public ElasticsearchContainer(boolean start) {
        super(ElasticsearchContainerSupport.OPENSEARCH_IMAGE);
        setExposedPorts(List.of(9200));
        // Warn when running on macOS ARM (Apple Silicon) because some container images or Testcontainers
        // setups may behave differently there.
        if (isMacArm()) {
            log.warn("""
                    WARNING: detected macOS on ARM (Apple Silicon). Testcontainers/OpenSearch may require special images or Rosetta.
                    This may require emulation, because no ARM image available for opensearch.
                    Things go better if you enable 'Use Rosetta for x86/amd64 emulation on Apple Silicon' in Docker Desktop settings.
                    See (nl.vpro.test.opensearch.ElasticsearchContainerSupport).
                    Perhaps also something like colima or podman may help (See https://java.testcontainers.org/supported_docker_environment/)
                    """);
        }
        if (start) {
            start();
        }

    }

    public  HttpHost getHttpHost() {
        return new HttpHost(getHost(), getMappedPort());
    }

    public int getMappedPort() {
        return getMappedPort(9200);
    }


    public String getClusterName() {
        return "opensearch";
    }

    private static boolean isMacArm() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        return (os.contains("mac") || os.contains("darwin")) && (arch.contains("aarch") || arch.contains("arm") || arch.contains("arm64"));
    }
}

package nl.vpro.test.opensearch;

import java.util.List;

import org.testcontainers.containers.GenericContainer;

public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {
    public ElasticsearchContainer(boolean start) {
        super(ElasticsearchContainerSupport.OPENSEARCH_IMAGE);
        setExposedPorts(List.of(9200));
        if (start) {
            start();
        }

    }

    public String getUnicastHosts() {
        return getHost() + ":" + getMappedPort();
    }

    public int getMappedPort() {
        return getMappedPort(9200);
    }


    public String getClusterName() {
        return "opensearch";
    }
}

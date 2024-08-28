package nl.vpro.test.opensearch;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

/**
 * Will set up a postgresql container bean for using spring, which can be injected in (spring based) tests like so:
 * <pre>
 * {@code
 * @ExtendWith(SpringExtension.class)
 * @ContextConfiguration(classes = {
 *     PostgresqlContainerSupport.class,
 * })
 * public class MyTest {
 * ...
 *
 * @Inject
 * private DataSoure dataSource;
 *
 * </pre>
 */
@Configuration
@Slf4j
public class ElasticsearchContainerSupport {

    public static final String OPENSEARCH_IMAGE = "ghcr.io/npo-poms/opensearch:opendistro";



    @Bean("elasticsearchContainer")
    public  ElasticsearchContainer getOpensearchContainer() {
        ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer();
        elasticsearchContainer.start();
        return elasticsearchContainer;
    }

    public static class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {
        public ElasticsearchContainer() {
            super(OPENSEARCH_IMAGE);
            setExposedPorts(List.of(9200));

        }
        public String getUnicastHosts() {
            return getHost() + ":" + getMappedPort(9200);
        }
        public String getClusterName() {
            return "opensearch";
        }
    }



}

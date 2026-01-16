package nl.vpro.test.opensearch;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Will set up a postgresql container bean for using spring, which can be injected in (spring based) tests like so:
 * <pre>
 * {@code
 * @ExtendWith(SpringExtension.class)
 * @ContextConfiguration(classes = {
 *     ElasticsearchContainerSupport.class,
 * })
 * public class MyTest {
 * ...
 *
 * @Inject
 * private ElasticsearchContainer elasticsearchContainer;
 *}
 * }
 * </pre>
 */
@Configuration
@Slf4j
public class ElasticsearchContainerSupport {

    public static final String OPENSEARCH_IMAGE = "ghcr.io/npo-poms/opensearch:opendistro";


    /**
     * Using a longer then default startup timeout, because there is no arm image, so on m1/m2 macs it has to emulate, and the start up may be quite clow.
     * @return A new ElasticsearchContainer with extended startup timeout, and started.
     */
    @Bean("elasticsearchContainer")
    public  ElasticsearchContainer getOpensearchContainer() {
        ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(false)
            .withStartupTimeout(Duration.ofMinutes(5));
        elasticsearchContainer.start();
        return elasticsearchContainer;
    }


}

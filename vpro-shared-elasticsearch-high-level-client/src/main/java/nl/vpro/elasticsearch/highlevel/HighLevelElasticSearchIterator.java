package nl.vpro.elasticsearch.highlevel;

import lombok.SneakyThrows;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.meeuw.math.windowed.WindowedEventRate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearchclient.ElasticSearchIterator;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 2.19
 */
public class HighLevelElasticSearchIterator<T> extends ElasticSearchIterator<T> {

    private SearchSourceBuilder searchSourceBuilder;

    @lombok.Builder(builderClassName = "HighLevelBuilder", builderMethodName = "highLevelBuilder")
    @lombok.SneakyThrows
    protected HighLevelElasticSearchIterator(
        @lombok.NonNull RestClient client,
        Function<JsonNode, T> adapt,
        Class<T> adaptTo,
        Duration scrollContext,
        Boolean jsonRequests,
        Boolean requestVersion,
        String beanName,
        WindowedEventRate rateMeasurerer,
        List<String> routingIds
    ) {
        super(
            client,
            adapt,
            adaptTo,
            scrollContext,
            null,
            false,
            jsonRequests,
            requestVersion,
            beanName,
            rateMeasurerer,
            routingIds);
    }

    public SearchSourceBuilder prepareSearchSource(String... indices)  {
        setIndices(Arrays.asList(indices));

        this.searchSourceBuilder = new SearchSourceBuilder();
        return this.searchSourceBuilder;
    }

    @SneakyThrows
    @Override
    protected boolean firstBatch() {
        if (searchSourceBuilder != null) {
            byte[] json =  XContentHelper.toXContent(searchSourceBuilder, XContentType.JSON, ToXContent.EMPTY_PARAMS, false).toBytesRef().bytes;
            ObjectNode jsonNode = (ObjectNode) Jackson2Mapper.getLenientInstance().readTree(json);
            request = jsonNode;
        }
        return super.firstBatch();
    }

    public static class HighLevelBuilder<T> extends ElasticSearchIterator.AbstractBuilder<T, HighLevelBuilder<T>> {
        public HighLevelBuilder() {

        }
    }


}

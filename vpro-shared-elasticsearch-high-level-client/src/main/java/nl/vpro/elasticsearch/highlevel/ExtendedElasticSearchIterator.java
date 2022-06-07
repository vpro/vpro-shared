package nl.vpro.elasticsearch.highlevel;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.meeuw.math.windowed.WindowedEventRate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.vpro.elasticsearchclient.ElasticSearchIterator;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * Like a low level {@link ElasticSearchIterator}, but with a method {@link #prepareSearchSource}, so queries can be
 * constructed using 'high level' syntax. All other things, like the type to adapt are still json only.
 * @author Michiel Meeuwissen
 * @since 2.19
 */
@Slf4j
public class ExtendedElasticSearchIterator<T> extends ElasticSearchIterator<T> {

    private SearchSourceBuilder searchSourceBuilder;

    @lombok.Builder(builderClassName = "ExtendedBuilder", builderMethodName = "extendedBuilder")
    @lombok.SneakyThrows
    protected ExtendedElasticSearchIterator(
        @lombok.NonNull RestHighLevelClient client,
        Function<JsonNode, T> adapt,
        Class<T> adaptTo,
        Duration scrollContext,
        Boolean jsonRequests,
        Boolean requestVersion,
        String beanName,
        WindowedEventRate rateMeasurerer,
        List<String> routingIds,
        Boolean warnSortNotOnDoc
    ) {
        super(
            client.getLowLevelClient(),
            adapt,
            adaptTo,
            scrollContext,
            null,
            false,
            jsonRequests,
            requestVersion,
            beanName,
            rateMeasurerer,
            routingIds,
            warnSortNotOnDoc);
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

    public static class ExtendedBuilder<T> extends ElasticSearchIterator.AbstractBuilder<T, ExtendedBuilder<T>> {
        public ExtendedBuilder() {

        }

    }


}

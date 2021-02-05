package nl.vpro.elasticsearch.highlevel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.management.ObjectName;

import org.apache.http.util.EntityUtils;
import org.apache.lucene.search.TotalHits;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.*;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.meeuw.math.windowed.WindowedEventRate;

import nl.vpro.elasticsearch.ElasticSearchIteratorInterface;
import nl.vpro.elasticsearchclient.ElasticSearchIterator;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jmx.MBeans;
import nl.vpro.util.ThreadPools;

/**
 * @author Michiel Meeuwissen
 * @since 2.22
 */
@Slf4j
public class HighLevelElasticSearchIterator<T> implements ElasticSearchIteratorInterface<T> {

     private static long instances = 0;

    @Getter
    private final long instance = instances++;

    private final Function<SearchHit, T> adapt;
    private final RestHighLevelClient client;

    @Getter
    private SearchResponse response;
    @Getter
    private Long count = -1L;
    private SearchHits hits;
    private String scrollId;
    private Long checkedOrder = 500L;

    private boolean hasNext;
    private int i = -1;
    private T next;
    private boolean needsNext = true;
    private String[] indices;

    @Getter
    @Setter
    private String[] routing;

    @Getter
    private Instant start;

    @Getter
    private Duration duration = Duration.ofMillis(0);


    private SearchSourceBuilder searchSourceBuilder;

    private final Duration scrollContext;


    private Long totalSize = null;
    private TotalRelation totalRelation = TotalRelation.EQUAL_TO;

    private final boolean requestVersion = true;

    @Getter
    private final WindowedEventRate rate;

    private final ObjectName objectName;

    @Getter
    @Setter
    private RequestOptions requestOptions;



    @lombok.Builder(builderClassName = "Builder")
    @lombok.SneakyThrows
    protected HighLevelElasticSearchIterator(
        @lombok.NonNull RestHighLevelClient client,
        Function<SearchHit, T> adapt,
        Class<T> adaptTo,
        Duration scrollContext,
        String beanName,
        WindowedEventRate rateMeasurerer,
        List<String> routingIds,
        RequestOptions requestOptions
    ) {
        this.adapt = adapterTo(adapt, adaptTo);
        this.client = client;
        this.scrollContext = scrollContext == null ? Duration.ofMinutes(1) : scrollContext;

        if (beanName != null) {
            objectName = MBeans.registerBean(this, instance + "-" + beanName);
        } else {
            objectName = null;
        }

        this.rate = rateMeasurerer == null ? WindowedEventRate.builder()
                .bucketCount(5)
                .bucketDuration(Duration.ofMinutes(1))
                .build() : rateMeasurerer;

        this.routing = routingIds == null ? null : routingIds.toArray(new String[0]);
        this.requestOptions = requestOptions == null ? RequestOptions.DEFAULT : requestOptions;
    }


    public static <T> Function<SearchHit, T> adapterTo(Class<T> clazz) {
        return searchHit -> {
            try {
                return Jackson2Mapper.getLenientInstance()
                    .readValue(searchHit.getSourceRef().toBytesRef().bytes, clazz);
            } catch (Exception e) {
                return null;

            }
        };
    }

    private static <T> Function<SearchHit, T> adapterTo(Function<SearchHit, T> adapter, Class<T> clazz) {
        if (adapter != null && clazz != null) {
            throw new IllegalArgumentException();
        }
        if (clazz != null) {
            return  adapterTo(clazz);
        }
        if (adapter == null) {
            return searchHit -> (T) searchHit;
        }
        return adapter;
    }


    public SearchSourceBuilder prepareSearchSource(String... indices) {
        this.indices = indices;
        this.searchSourceBuilder = new SearchSourceBuilder();
        return this.searchSourceBuilder;
    }

    public SearchSourceBuilder getRequest() {
        if (searchSourceBuilder == null) {
            throw new IllegalStateException("prepareSearch not called");
        }
        return searchSourceBuilder;
    }

    @Override
    public boolean hasNext() {
        findNext();
        return hasNext;

    }

    protected void findNext() {
        if (needsNext) {
            synchronized (this) {
                long start = System.nanoTime();
                try {
                    if (response == null) {
                        if (! firstBatch()) {
                            return;
                        }
                    }
                    i++;
                    boolean newHasNext = i < hits.getHits().length;
                    if (!newHasNext) {
                        nextBatch();
                    } else {
                        hasNext = true;
                    }
                    if (hasNext) {
                        next = adapt.apply(hits.getHits()[i]);
                    } else {
                        close();
                    }
                    needsNext = false;
                } finally {
                    duration = duration.plusNanos(System.nanoTime() - start);
                }
            }
        }
    }

    @Override
    public float getFraction() {
        long total = Duration.between(start, Instant.now()).toMillis();
        long es = duration.toMillis();
        return (float) es / total;
    }

    protected boolean firstBatch() {
        // first call only.
        if (searchSourceBuilder == null) {
            throw new IllegalStateException("prepareSearch not called");
        }
        if (client == null) {
            throw new IllegalStateException("No client");
        }
        try {
            SearchRequest searchRequest = new SearchRequest(indices, searchSourceBuilder);
            start = Instant.now();
            searchRequest.scroll(getScroll());
            response = client.search(searchRequest, requestOptions);

        } catch (IOException ioe) {
            //log.error(ioe.getMessage());
            throw new RuntimeException("For request " + searchSourceBuilder.toString() + ":" + ioe.getMessage(), ioe);

        }
        if (hits == null) {
            readResponse();
        }
        String newScrollId = response.getScrollId();
        if (newScrollId != null) {
            log.debug("Scroll id {} -> {}", scrollId, newScrollId);
            scrollId = newScrollId;
            SCROLL_IDS.add(scrollId);
        }

        TotalHits total = hits.getTotalHits();
        totalSize = total.value;
        if (totalSize == 0) {
            hasNext = false;
            needsNext = false;
            close();
            return false;
        }
        return true;

    }

    private Scroll getScroll() {
        if (! scrollContext.isNegative()) {
            return new Scroll(new TimeValue(scrollContext.toMillis(), TimeUnit.MILLISECONDS));
        } else {
            return new Scroll(new TimeValue(30, TimeUnit.SECONDS));
        }
    }

    private void nextBatch() {
        if (scrollId != null) {
            try {
                if( count > checkedOrder) {
                    checkedOrder = Long.MAX_VALUE;
                }

                SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId);
                searchScrollRequest.scroll(getScroll());
                response = client.scroll(searchScrollRequest, requestOptions);
                log.debug("New scroll");
                String newScrollId = response.getScrollId();
                if (!scrollId.equals(newScrollId)) {
                    log.info("new scroll id {}", newScrollId);
                    SCROLL_IDS.remove(scrollId);
                    scrollId = newScrollId;
                    SCROLL_IDS.add(scrollId);
                }
                readResponse();
                i = 0;
                hasNext = hits.getHits().length > 0;
            } catch(ResponseException re) {
                log.warn(re.getMessage());
                hits = null;
                hasNext = false;
            } catch (IOException ioe) {
                log.error(ioe.getMessage());
                throw new RuntimeException("For request " + searchSourceBuilder.toString() + ":" + ioe.getMessage(), ioe);
            }
        } else {
            log.warn("No scroll id found, so not possible to scroll next batch");
            hasNext = false;
        }
    }

    protected void readResponse() {
        hits = response.getHits();
        if (hits != null) {
            TotalHits total  = hits.getTotalHits();
            totalSize = total.value;
        }
    }

    @Override
    public T next() {
        findNext();
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        count++;
        needsNext = true;
        rate.newEvent();
        return next;
    }


    @Override
    public @NonNull Optional<Long> getSize() {
        findNext();
        return Optional.ofNullable(this.totalSize);
    }

    @Override
    public Optional<TotalRelation> getSizeQualifier() {
        findNext();
        if (hits != null) {
            TotalHits total  = hits.getTotalHits();
            this.totalRelation = TotalRelation.valueOf(total.relation.name());
            this.totalSize = total.value;
        }
        return Optional.ofNullable(this.totalRelation);
    }


    @Override
    public String toString() {
        return client + " " + searchSourceBuilder + " " + count;
    }


    @Override
    public void close()  {
        if (objectName != null) {
            ThreadPools.backgroundExecutor.schedule(() -> MBeans.unregister(objectName), 2, TimeUnit.MINUTES);
        }
        if (scrollId != null) {
            try {
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(scrollId);

                ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);

                if (clearScrollResponse.isSucceeded()) {
                    log.debug("Deleted {} {}", scrollId, clearScrollResponse);
                    SCROLL_IDS.remove(scrollId);
                } else {
                    log.warn("Something wrong deleting scroll id {} {}", scrollId, clearScrollResponse);
                }
                scrollId = null;
            } catch (ResponseException re) {
                if (re.getResponse().getStatusLine().getStatusCode() == 404) {
                    log.debug("Not found to delete");
                } else {
                    log.warn(re.getMessage());
                }
                EntityUtils.consumeQuietly(re.getResponse().getEntity());
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        } else {
            log.debug("no need to close");
        }
    }

    @Override
    public double getSpeed() {
        return rate.getRate();
    }


    public static class Builder<T> extends ElasticSearchIterator.AbstractBuilder<T, HighLevelElasticSearchIterator.Builder<T>>  {

        public Builder() {

        }

    }
}

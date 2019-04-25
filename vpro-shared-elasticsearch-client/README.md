[![javadoc](http://www.javadoc.io/badge/nl.vpro.shared/vpro-shared-elasticsearch-client.svg?color=blue)](http://www.javadoc.io/doc/nl.vpro.shared/vpro-shared-elasticsearch-client)

# Elasticsearch utils

Depends only on [elastic search restclient](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/index.html). No lucene dependencies.

It constains several simple utilities. These are some important ones

## IndexerHelper
Helps creating and maintaining indexes.
```java
  client = RestClient.builder(
            new HttpHost("localhost", 9200, "http"))
            .build();

  helper = IndexHelper.builder()
            .log(log)
            .client((e) -> client)
            .settingsResource("setting.json")
            .mappingResource("test.json")
            .indexName("test-" + System.currentTimeMillis())
            .build();
        
  helper.createIndexIfNotExists();
  
  ..
  helper.deleteIndex()
        
```
We use this in test classes, but also in some repository implementations to bootstrap databases if they happen to not exist yet.

## ElasticSearchIterator
Wraps the scroll interface in an `java.util.Iterator`.
```java
 ElasticSearchIterator<JsonNode> i = ElasticSearchIterator.sources(client);
 JsonNode search = i.prepareSearch("pageupdates-publish");
 // fill your request here
 i.forEachRemaining((node) -> {
     String url = node.get("url").textValue();
     if (i.getCount() % 1000 == 0) {
         log.info("{}: {}", i.getCount(), url);
     }
 });

```
The idea is to create it with a client, then 'prepare the search' (you may use e.g. utilities from `nl.vpro.elasticsearchclient.QueryBuilder` to do that).

You can also use an adapter, to have an iterator of other object types. It is e.g. possible to use an ObjectMapper and have an iterator of your domain objects.

Here is a more full example, which shows how to 'prepare' the search, and how you could use an adapter.
```java
 ElasticSearchIterator<String> i = ElasticSearchIterator
            .<String>builder()
            .client(client)
            .adapt(jsonNode -> jsonNode.get(Constants.ID).textValue())
            .build();
        ObjectNode search = i.prepareSearch("pageupdates-publish");
        QueryBuilder.asc(search, "lastPublished");
        ObjectNode query = search.with(Constants.QUERY);
        QueryBuilder.mustTerm(query, "broadcasters", "VPRO");

        i.forEachRemaining((u) -> {
            log.info("{}/{}: {} (eta: {})", i.getCount(), i.getTotalSize().orElse(null), u,
                i.getETA().map(eta -> eta.atZone(ZoneId.of("Europe/Amsterdam")).toLocalDateTime()).orElse(null)
            );
        });
```



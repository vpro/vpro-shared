# Elasticsearch utils

Depends only on elastic search restclient. No lucene dependencies.

## IndexerHelper
Helps creating and mainting indexer
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
        

```
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

You can also use an adapter:
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



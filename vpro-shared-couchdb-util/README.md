couchdb
=======

Lightweight straight forward streaming couchdb client implementation in java.

Access to a couchdb view is implemented as an Iterator. Just feed it an input stream and than you can iterate the view 
as Jackson JsonNode's:
```java
 InputStream inputStream = CouchdbViewIterator.class.getResourceAsStream("/alldocs.json");
 CouchdbViewIterator iterator = new CouchdbViewIterator(inputStream);      
 while(iterator.hasNext()) {
    JsonNode node = iterator.next();
    ....
 }
```
This example is from a test case. Normally you would open the stream using HTTP. To help constructing the URL for that, 
a 'CouchdbOptions' object is present too (which is copied from org.jcouchdb.db.Options, but with removed dependencies), 
and also a 'CouchdView' class.

Some pseudo code:
```java
   
   private static final CouchdbView view = new CouchdbView("media", "by-parent-and-type");
   
   
   public Iterator<MyObject> getMyObjects(String id) {
        CouchdbOptions options = new CouchdbOptions()
                    .reduce(false)
                    .includeDocs(true)
                    .startKey(new Object[]{id, null})
                    .endKey(new Object[]{id, new Object[]{}});
        URL url = new URL(couchdbUrlprovider + database + "/" + view.toString() + options.toQuery());
        return new JsonNodeToMyObjectAdapter(new CouchdViewIterator(url.openStream()));
                    
   }
   

```

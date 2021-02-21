[![javadoc](http://www.javadoc.io/badge/nl.vpro.shared/vpro-shared-util.svg?color=blue)](http://www.javadoc.io/doc/nl.vpro.shared/vpro-shared-util)


# Shared utilities

Simple utilities with little or no dependencies themselves

E.g. 
- Collection related, like CountedIterator, ClosableIterator,  SkippingIterator, TransformingList, BatchedReceiver
- Strings related, like TextUtil, HTMLStripper, Strings
- JMX related: `nl.vpro.jmx`
- CommandExecutor to convientiently call external commands
- ...

## CommandExecutor
```java
 // initialize
 CommandExecutor convert = CommandExecutorImpl
                 .builder()
                 .executablesPaths("/opt/local/bin/convert", "/bin/convert", "/usr/local/bin/convert")
                 .commonArg("-")
                 .commonArg("png:-")
                 .logger(log)
                 .wrapLogInfo(CharSequence::toString)
                 .build();
  
  
  //use 
  File out = File.createTempFile(FilenameUtils.removeExtension(pdfFileName), ".png");
  try (OutputStream outputStream = new FileOutputStream(out)) {
      int exitCode = convert.execute(zip, outputStream, LoggerOutputStream.error(log));
      if (exitCode != 0) {
           log.warn("Exit code: {}", exitCode);
      }
  }
```
## Batched Receiver

If an API provides access to huge set of elements, they often do it with some paging mechanism, or by some 'resumption token' formalism. With `nl.vpro.util.BatchedReceiver` this can be morphed into a simple `java.util.Iterator`.

### Paging
The 'batchGetter' argument should be a `java.util.BiFunction`, returning an iterator for the page described by given offset and batch size
```java
Iterator<String> i = BatchedReceiver.<String>builder()
    .batchGetter((offset, max) ->
       apiClient.getPage(offset, max).iterator()
    )
    .batchSize(6)
    .build();
i.forEachRemaining(string -> {
      ...<do stuff...>
  });
```
### Resumption token
You simply provide a `java.util.Supplier`. A lambda would probably not suffice because you might need the previous result the get the next one. E.g. this (using [olingo](https://olingo.apache.org/doc/odata4/index.html) code) 
```java
   public Iterator<ClientEntity> iterate(URIBuilder ub) {
        return BatchedReceiver.<ClientEntity>builder()
            .batchGetter(new Supplier<Iterator<ClientEntity>>() {
                ClientEntitySet result;
                @Override
                public Iterator<ClientEntity> get() {
                    if (result != null) {
                        result = query(result.getNext());
                    } else {
                        result = query(ub);
                    }
                    return result.getEntities().iterator();
                }
            })
            .build();
    }
```

## Configuration utility
At vpro we use to place configuration properties file for all project on similar places.

The can be obtained like this:
```java
nl.vpro.util.ConfigUtils#getPropertiesInHome("<my config file>.properties");
```
Resolves properties in class path and `user.home`/conf in a standardized way.

We also support a convention to have properties differ per OTAP environment
```java
  Map<String, String> properties =
        ConfigUtils.filtered(env, ConfigUtils.getPropertiesInHome("openskosrepository.properties"));
```

It is also possible to instantiate obects using reflection.
```java
TODO device examples
```
 

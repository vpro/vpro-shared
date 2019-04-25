[![javadoc](http://www.javadoc.io/badge/nl.vpro.shared/vpro-shared-jackson2.svg?color=blue)](http://www.javadoc.io/doc/nl.vpro.shared/vpro-shared-jackson2)

# Jackson2 utilities

We collect some generic Jackson2 utilities. Mainly `com.fasterxml.jackson.databind.JsonSerializer`s and `com.fasterxml.jackson.databind.JsonDeserializer`s.

Some of them are bundled in modules. E.g. a `nl.vpro.jackson2.DateModule`, which can will make a `com.fasterxml.jackson.databind.ObjectMapper` recognize `java.time` classes
(but a bit differently then `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule` does, which it predates).

Also `nl.vpro.jackson2.Views` is provided which defines a few classes which can be used with `@com.fasterxml.jackson.annotation.JsonView`.

## JsonArrayIterator

This package also contains `nl.vpro.jackson2.JsonArrayIterator`. A tool to wrap, using jackson, a stream of json object into an iterator of java objects.

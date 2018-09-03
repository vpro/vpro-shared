# VPRO Shared logging

Some utilities related to logging.

E.g.:
1.  `nl.vpro.logging.LoggerOutputStream` Wraps a logger (either a `java.util.logging.Logger`, a `org.slf4j.Logger` or a `nl.vpro.logging.simple.SimpleLogger`) into an OutputStream. 
    If something accepts an outputstream (e.g. an external command) to write feed back to, you can wrap a logger in this to arrange that that output appears in your log.
    
2. `nl.vpro.logging.Slf4jHelper` makes it possible to log with the `org.slf4j.event.Level` as an argument

3. The `nl.vpro.logging.simple` contains a simple logging framework which main goal is to be simple to implement, so that it is easy to chain and 'tee' logging which simple lines of java code.

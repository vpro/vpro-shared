/**
 * Available logging frameworks are sometimes a bit hard. The de facto standard at vpro is slf4j. Its interface is however hard to
 * completely implement. {@link nl.vpro.logging.simple.SimpleLogger} is much easier. A complete implementation may be just a few lines.
 *
 * Performance is not a key goal of this framework. The main goal is easy of implementation. The basic idea is that a logger is not
 * much more then a consumer of messages. These could be written to some logger framework, but sometimes you want to do something else
 * with it too.
 *
 * E.g. collecting it in JMX calls, or throwing it on web sockets so that web site users can see them too.
 *
 * A {@link nl.vpro.logging.simple.SimpleLogger} therefor is a good choice if methods use some Logger as an argument. The method is somewhat restricted, but the caller can decide what to do with logging very easily.
 *
 * @author Michiel Meeuwissen
 * @since 1.77
 */
package nl.vpro.logging.simple;

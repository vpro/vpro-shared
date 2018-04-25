/**
 * This package contains a simplified logger framework. Using this framework you submit to these points:
 * - You dont really care about optimal performance
 * - You don't really care about simplicity
 * - You want to be able to easily chain and implement your own loggers.
 *
 *
 * The idea is that a logger is not much more then a consumer of messages.
 * These should be written to some logger framework, but sometimes you want to do something else with it too.
 *
 * E.g. collecting it in JMX calls, or throwing it on web sockets so that web site users can see them too.
 *
 * @author Michiel Meeuwissen
 * @since 1.77
 */
package nl.vpro.logging.simple;

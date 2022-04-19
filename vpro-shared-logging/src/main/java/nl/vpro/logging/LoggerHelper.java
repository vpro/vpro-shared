/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.logging;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.ext.LoggerWrapper;

/**
 * An SFL4j-logger wrapper that add an timestamp to every trace message.
 */

public final class LoggerHelper extends LoggerWrapper {

    private final Clock clock;
    public LoggerHelper(Logger logger, Clock clock) {
        super(logger, logger.getName());
        this.clock = clock;
    }

    public LoggerHelper(Logger logger) {
        this(logger, Clock.systemUTC());
    }

    public static void trace(Logger logger, String message, Object... args) {
        trace(Clock.systemUTC(), logger, message, args);
    }

     public static void trace(Clock clock, Logger logger, String message, Object... args) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%1$tT.%1$tL - %2$s", clock.millis(), message), args);
        }
    }

    @Override
    public void trace(String message, Object... args) {
        LoggerHelper.trace(clock, logger, message, args);
    }

    @Override
    public void trace(String message, Object arg) {
        LoggerHelper.trace(clock, logger, message, arg);
    }
}

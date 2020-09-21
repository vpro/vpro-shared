/*
 * Copyright (C) 2010 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.logging.jmx;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(
        objectName="nl.vpro:name=logging",
        description="Logging configuration.",
        log=true,
        logFile="jmx.log"
)
public class Log4jMBean {

    @ManagedOperation(description="Get current level for category")
    public String getLevel(String name) {
        return LogManager.getLogger(name).getLevel().toString();
    }

    @ManagedOperation(description="Level trace")
    public void trace(String name) {
        LogManager.getLogger(name).setLevel(Level.TRACE);
    }

    @ManagedOperation(description="Level debug")
    public void debug(String name) {
        LogManager.getLogger(name).setLevel(Level.DEBUG);
    }

    @ManagedOperation(description="Level info")
    public void info(String name) {
        LogManager.getLogger(name).setLevel(Level.INFO);
    }

    @ManagedOperation(description="Level warn")
    public void warn(String name) {
        LogManager.getLogger(name).setLevel(Level.WARN);
    }

    @ManagedOperation(description="Level error")
    public void error(String name) {
        LogManager.getLogger(name).setLevel(Level.ERROR);
    }

    @ManagedOperation(description="Level fatal")
    public void fatal(String name) {
        LogManager.getLogger(name).setLevel(Level.FATAL);
    }
}

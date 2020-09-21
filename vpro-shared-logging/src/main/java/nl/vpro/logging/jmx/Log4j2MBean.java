/*
 * Copyright (C) 2020 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.logging.jmx;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(
        objectName="nl.vpro:name=logging",
        description="Logging configuration (log4j 2)",
        log=true,
        logFile="jmx.log"
)
public class Log4j2MBean {

    @ManagedOperation(description="Get current level for category")
    public String getLevel(String name) {
        return LogManager.getLogger(name).getLevel().toString();
    }

    @ManagedOperation(description="Level trace")
    public void trace(String name) {
        Configurator.setLevel(name, Level.TRACE);
    }

    @ManagedOperation(description="Level debug")
    public void debug(String name) {
        Configurator.setLevel(name, Level.DEBUG);
    }

    @ManagedOperation(description="Level info")
    public void info(String name) {
        Configurator.setLevel(name, Level.INFO);
    }

    @ManagedOperation(description="Level warn")
    public void warn(String name) {
        Configurator.setLevel(name, Level.WARN);
    }

    @ManagedOperation(description="Level error")
    public void error(String name) {
        Configurator.setLevel(name, Level.ERROR);
    }

    @ManagedOperation(description="Level fatal")
    public void fatal(String name) {
        Configurator.setLevel(name, Level.FATAL);
    }
}

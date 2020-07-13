package nl.vpro.jmx;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Constructor;
import java.time.Duration;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.jmx.export.annotation.*;

import nl.vpro.logging.simple.SimpleLogger;

/**
 * @author Michiel Meeuwissen
 * @since 3.0
 */
@ManagedResource(description = "Execute groovy script", objectName = "nl.vpro:name=groovy")
@Slf4j
public class GroovyMBean {

    @Inject
    ApplicationContext applicationContext;


    GroovyObject currentlyRunning;

    @ManagedOperation(description = "Execute Groovy Script", currencyTimeLimit= -1)
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "groovyScript", description = ""),
        @ManagedOperationParameter(name = "arguments", description = "")}
    )
    public String executeGroovySript(String groovyScript, String arguments) {
        return
            MBeans.returnString(groovyScript,  MBeans.multiLine(log, groovyScript), Duration.ofSeconds(10), (logger) -> {
                try {
                    ClassLoader parent = getClass().getClassLoader();
                    GroovyClassLoader loader = new GroovyClassLoader(parent);

                    File file = new File(groovyScript);
                    Class<GroovyObject> groovyClass;
                    if (file.exists()) {
                        logger.info("{} is a file, so interpreting the file", groovyScript);
                        groovyClass = loader.parseClass(file);
                    } else {
                        groovyClass = loader.parseClass(groovyScript);
                    }
                    GroovyObject groovyObject;
                    try {
                        Constructor<GroovyObject> constructor = groovyClass.getConstructor(ApplicationContext.class, SimpleLogger.class);

                        groovyObject = constructor.newInstance(applicationContext, logger);
                    } catch (NoSuchMethodException nsme) {
                        groovyObject = groovyClass.newInstance();
                    }
                    Object[] args = arguments.split(",");
                    currentlyRunning = groovyObject;
                    groovyObject.invokeMethod("run", args);

                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    currentlyRunning = null;
                }
            });
    }


    @ManagedOperation(description = "Interrupt Groovy Script")
    public String interrupt() {
        if (currentlyRunning != null) {
            AbstractGroovyScript abstractGroovyScript = (AbstractGroovyScript) currentlyRunning;
            abstractGroovyScript.interrupted = true;
            return "Interrupted " + currentlyRunning;
        } else {
            return "No current script running";
        }
    }
}

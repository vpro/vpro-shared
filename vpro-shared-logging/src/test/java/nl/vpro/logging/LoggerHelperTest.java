/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.logging;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class LoggerHelperTest {
    private static final Logger log = LoggerFactory.getLogger(LoggerHelperTest.class);

    @Test
    public void trace() {
        final StringWriter writer = new StringWriter();


        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        LoggerConfig rootLogger = config.getRootLogger();
        WriterAppender appender = WriterAppender.createAppender(new AbstractStringLayout(StandardCharsets.UTF_8) {
            @Override
            public String toSerializable(LogEvent event) {
                return event.getLevel() + " " + event.getMessage().getFormattedMessage();
            }
        }, null, writer, "test", true, false);
        appender.start();
        rootLogger.addAppender(appender, Level.TRACE, null);
        rootLogger.setLevel(Level.TRACE);

        ctx.updateLoggers();

        LoggerHelper helper = new LoggerHelper(log);
        helper.trace("message {}", "argument");

        assertThat("123:bbb - ").matches("^[0-9]{3}:[b]{3} \\- $");
        assertThat(writer.toString()).matches("^TRACE \\- [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3} \\- message argument\\n$");

        LoggerHelper.trace(log, "message {}", "argument");
        assertThat(writer.toString()).matches("^TRACE \\- [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3} \\- message argument\\nTRACE \\- [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3} \\- message argument\\n$");
    }
}

/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.logging;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;
import org.meeuw.time.TestClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

// logging configuration is reused by other tests, so run this isolated.
@Isolated
@Execution(SAME_THREAD)
class LoggerHelperTest {
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
                return event.getLevel() + " " + event.getMessage().getFormattedMessage() + "\n";
            }
        }, null, writer, "test", true, false);
        appender.start();
        rootLogger.addAppender(appender, Level.TRACE, null);
        Level prev = rootLogger.getLevel();
        try {
            rootLogger.setLevel(Level.TRACE);

            ctx.updateLoggers();

            TestClock clock = new TestClock(ZoneId.of("UTC"), Instant.parse("2022-04-19T19:00:00Z"));
            LoggerHelper helper = new LoggerHelper(log, clock);
            helper.trace("message {}", "argument");

            assertThat(writer.toString()).isEqualTo("TRACE 19:00:00.000 - message argument\n");

            clock.tick(Duration.ofSeconds(100));
            LoggerHelper.trace(clock, log, "message {}", "argument2");
            assertThat(writer.toString()).isEqualTo("TRACE 19:00:00.000 - message argument\n" +
                "TRACE 19:01:40.000 - message argument2\n");
            rootLogger.removeAppender("test");
        } finally {
            rootLogger.setLevel(prev);
        }

    }
}

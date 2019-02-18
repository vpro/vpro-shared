package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import nl.vpro.logging.simple.SimpleLogger;
import nl.vpro.logging.simple.StringBuilderSimpleLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class CommandExecutorImplTest {

    @Rule
    public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);


    @Test
    public void execute() {
        CommandExecutorImpl instance = new CommandExecutorImpl("/usr/bin/env");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        instance.execute(out, "echo", "hoi");
        assertEquals("hoi\n", new String(out.toByteArray()));

        instance.execute("echo", "hoi");
    }

    @Test
    public void logger() {
        SimpleLogger logger = new CommandExecutorImpl("/usr/bin/env").getLogger();
        assertEquals(CommandExecutorImpl.class.getName() + ".env.bin.usr", logger.getName());
    }
    @Test
    public void lines() {
        CommandExecutor find =
            CommandExecutorImpl.builder()
                .executablesPaths("/usr/bin/env")
                .commonArg("find")
                .optional(true)
                .build();
        find.lines(".")
            .limit(20)
            .forEach(log::info);
    }


    @Test
    public void optional() throws IOException, InterruptedException {
        File tmpFile = File.createTempFile("env", "test");
        tmpFile.deleteOnExit();
        StringBuilderSimpleLogger logger = new StringBuilderSimpleLogger();
        CommandExecutor find =
            CommandExecutorImpl.builder()
                .executablesPaths(tmpFile.getAbsolutePath())
                .commonArg("find")
                .simpleLogger(logger)
                .optional(true)
                .build();
        find.lines(".")
            .limit(20)
            .forEach(log::info)
            ;
        // wait for whenComplete
        Thread.sleep(100);
        assertThat(logger.getStringBuilder().toString()).isEqualTo("ERROR java.lang.IllegalStateException: No binary found");

        logger.getStringBuilder().setLength(0);
        Files.copy(Paths.get(new File("/usr/bin/env").toURI()), Paths.get(tmpFile.toURI()), StandardCopyOption.REPLACE_EXISTING);

        find.lines(".")
            .limit(20)
            .forEach(log::info);
        assertThat(logger.getStringBuilder().toString()).doesNotContain("ERROR java.lang.IllegalStateException");
    }

    @Test
    public void workdir() {
        File workDir = new File("/tmp");
        CommandExecutorImpl instance = CommandExecutorImpl.builder().executablesPath("/bin/pwd")
            .workdir(workDir).build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        instance.execute(out);
        String actual = new String(out.toByteArray()).trim();
        if (actual.startsWith("/private")) {
            actual = StringUtils.substringAfter(actual, "/private");
        }
        assertEquals(workDir.getAbsolutePath(), actual);
        log.info("Found workdir {}", actual);
    }
}

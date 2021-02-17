package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;

import nl.vpro.logging.simple.SimpleLogger;
import nl.vpro.logging.simple.StringBuilderSimpleLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
@Timeout(value = 10, unit = TimeUnit.SECONDS)
@Isolated
@Execution(SAME_THREAD)
public class CommandExecutorImplTest {


    @AfterEach
    public void check() throws InterruptedException {
        synchronized (FileCachingInputStream.openStreams) {
            if (FileCachingInputStream.openStreams.get() != 0) {
                FileCachingInputStream.openStreams.wait(1000);
            }
        }
        assertThat(FileCachingInputStream.openStreams.get()).isEqualTo(0);
    }

    @Test
    public void execute() {
        CommandExecutorImpl instance = new CommandExecutorImpl("/usr/bin/env");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        instance.execute(out, "echo", "hoi");
        assertEquals("hoi\n", out.toString());
        instance.execute("echo", "hoi");
    }

    @Test
    public void logger() {
        SimpleLogger logger = new CommandExecutorImpl(new File("/usr/bin/env")).getLogger();
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
        String actual = out.toString().trim();
        if (actual.startsWith("/private")) {
            actual = StringUtils.substringAfter(actual, "/private");
        }
        assertEquals(workDir.getAbsolutePath(), actual);
        log.info("Found workdir {}", actual);

        assertThatThrownBy(() -> CommandExecutorImpl.builder().executablesPath("/bin/pwd")
            .workdir(new File("/foobar")).build()).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CommandExecutorImpl(new File("/bin/pwd"), new File("/foobar"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void builder() {
        CommandExecutorImpl build = CommandExecutorImpl.builder()
            .executablesPath("a")
            .executable(new File("b"))
            .executablesPaths("c", "d")
            .optional(true)
            .build();
        assertThat(build.getBinary().toString()).isEqualTo("[a, b, c, d]");

    }
}

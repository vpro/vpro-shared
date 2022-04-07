package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.vpro.logging.simple.*;

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

    @BeforeEach
    public void setup() {
        FileCachingInputStream.openStreams.set(0);
    }

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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void lines(boolean useFileCache) {
        CommandExecutor find =
                 CommandExecutorImpl.builder()
                     .executablesPaths("/usr/bin/env")
                     .commonArg("find", "-s")
                     .useFileCache(useFileCache)
                     .optional(true)
                     .build();
        assertThat(find.toString()).isEqualTo("/usr/bin/env find -s");
        try (Stream<String> s = find.lines(".")
            .limit(20)) {
            s.forEach(log::info);
        }
    }


    @Test
    public void optional() throws IOException, InterruptedException {
        final File tmpFile = File.createTempFile("env", "test");
        tmpFile.deleteOnExit();
        final StringBuilderSimpleLogger logger = new StringBuilderSimpleLogger();
        final CommandExecutor find =
            CommandExecutorImpl.builder()
                .executablesPaths(tmpFile.getAbsolutePath())
                .commonArg("find")
                .simpleLogger(logger)
                .wrapLogInfo(s -> "X:" + s)
                .optional(true)
                .build();
        find.lines(".")
            .limit(20)
            .forEach(log::info)
        ;
        // wait for whenComplete
        Thread.sleep(100);
        assertThat(logger.getStringBuilder().toString()).isEqualTo("ERROR X:java.lang.IllegalStateException: No binary found");

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
    public void timeout() {
        StringBuilderSimpleLogger sl = StringBuilderSimpleLogger.builder()
            .level(Level.ERROR)
            .build();
        CommandExecutorImpl instance = CommandExecutorImpl.builder()
            .executablesPath("/usr/bin/env")
            .simpleLogger(sl)
            .processTimeout(Duration.ofMillis(1))
            .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int result = instance.execute(out, "sleep", "10");
        assertThat(result).isEqualTo(143);
        assertThat(sl.get()).matches("ERROR Exit code 143 for calling /usr/bin/env sleep 10");
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

    @Test
    public void invalidConstruction() throws IOException {
        new File("/tmp/foobar").delete();
        assertThatThrownBy(() -> new CommandExecutorImpl(new File("/tmp/foobar"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CommandExecutorImpl(new File("/tmp/"), null)).isInstanceOf(IllegalArgumentException.class);
        new File("/tmp/pietjepuk").createNewFile();
        assertThatThrownBy(() -> new CommandExecutorImpl(new File("/tmp/pietjepuk"), null)).isInstanceOf(IllegalArgumentException.class);
    }



    @Test
    public void commonArgsSupplier() {
        AtomicLong i = new AtomicLong(0);
        CommandExecutor test =
                 CommandExecutorImpl.builder()
                     .executablesPaths("/usr/bin/env")
                     .commonArgsSupplier((Supplier<String>) () -> String.valueOf(i.get()))
                     .commonArg("a")
                     .commonArgs(Arrays.asList("a1", "a2"))
                     .commonArg(() -> "b")
                     .commonArg(() -> i)
                     .commonArgs(Arrays.asList("c", "d"))
                     .optional(true)
                     .build();
        i.set(100);
        assertThat(test.toString()).isEqualTo("/usr/bin/env 100 a a1 a2 b 100 c d");

    }
}

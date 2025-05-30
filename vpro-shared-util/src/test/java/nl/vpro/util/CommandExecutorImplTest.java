package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.logging.simple.*;

import static java.nio.charset.StandardCharsets.UTF_8;
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
                     .commonArg("find")
                     .useFileCache(useFileCache)
                     .optional(true)
                     .build();
        assertThat(find.toString()).isEqualTo("/usr/bin/env find");
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
        assertThat(logger.getStringBuilder().toString()).startsWith("ERROR X:nl.vpro.util.NoBinaryFound: No binary found");

        logger.getStringBuilder().setLength(0);
        Files.copy(Paths.get(new File("/usr/bin/env").toURI()), Paths.get(tmpFile.toURI()), StandardCopyOption.REPLACE_EXISTING);

        find.lines(".")
            .limit(20)
            .forEach(log::info);
        assertThat(logger.getStringBuilder().toString()).doesNotContain("ERROR nl.vpro.util.NoBinaryFound");
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
        assertThatThrownBy(() -> new CommandExecutorImpl(new File("/tmp/foobar"), null)).isInstanceOf(NoBinaryFound.class);
        assertThatThrownBy(() -> new CommandExecutorImpl(new File("/tmp/"), null)).isInstanceOf(NoBinaryFound.class);
        new File("/tmp/pietjepuk").createNewFile();
        assertThatThrownBy(() -> new CommandExecutorImpl(new File("/tmp/pietjepuk"), null)).isInstanceOf(NoBinaryFound.class);
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


    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    public void noCloseStreams(boolean useFile) {
        CommandExecutor cat =
            CommandExecutorImpl.builder()
                .executablesPaths("/bin/cat")
                .simpleLogger(Slf4jSimpleLogger.of(log))
                .useFileCache(useFile)
                .closeStreams(false)
                .batchSize(2)
                .build();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(getZip()))
        ) {
            while(true) {
                ZipEntry e = input.getNextEntry();
                if (e == null) {
                    break;
                }
                log.info(e.getName());
                OutputStream err = LoggerOutputStream.error(log, true);
                cat.execute(input, output, err);
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        assertThat(output.toString()).isEqualTo("aabb");
        log.info("Ready");
    }

    byte[] getZip() throws IOException {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {

            ZipEntry a = new ZipEntry("a");
            a.setComment("first entry");
            zip.putNextEntry(a);
            zip.write("aa".getBytes(UTF_8));
            zip.closeEntry();
            ZipEntry b = new ZipEntry("b");
            b.setComment("second entry");
            zip.putNextEntry(b);
            zip.write("bb".getBytes(UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();

    }

    @Test
    public void events() throws InterruptedException {
        CommandExecutor find =
            CommandExecutorImpl.builder()
                .executablesPaths("/usr/bin/env")
                .commonArg("find")
                .optional(true)
                .build();
        final List<CharSequence> events = new ArrayList<>();
        CompletableFuture<Integer> submit = find.submit(CommandExecutor.parameters()
            .arg("/")
            .outputConsumer(e -> {
                synchronized (events) {
                    events.add(e.getMessage());
                    events.notifyAll();
                }
            }));

        synchronized (events) {
            while (events.size() < 3) {
                events.wait();
                log.info("{} {}", events.size(), events.get(events.size() -1));
            }
        }
        assertThat(events.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void commandToString() {
        assertThat(CommandExecutorImpl.commandToString(Arrays.asList("ls", "/tmp/foo bar"))).isEqualTo("ls \"/tmp/foo bar\"");

        assertThat(CommandExecutorImpl.commandToString(Arrays.asList("ls", "/tmp/foo\"bar"))).isEqualTo("ls /tmp/foo\\\"bar");
    }
}

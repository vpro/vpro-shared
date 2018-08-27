package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import nl.vpro.logging.simple.SimpleLogger;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class CommandExecutorImplTest {

    @Rule
    public Timeout timeout = new Timeout(10, TimeUnit.MINUTES);


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
                .build();
        find.lines(".")
            .limit(20)
            .forEach(log::info);
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

package nl.vpro.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 */
public class CommandExecutorImplTest {

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
        Logger logger = new CommandExecutorImpl("/usr/bin/env").getLogger();
        assertEquals(CommandExecutorImpl.class.getName() + ".env.bin.usr", logger.getName());
    }
    @Test
    public void lines() {
        CommandExecutorImpl instance = new CommandExecutorImpl("/usr/bin/env");
        instance.lines("find",".").forEach(System.out::println);
    }

    @Test
    public void workdir() {
        File workDir = new File("/tmp");
        CommandExecutorImpl instance = new CommandExecutorImpl("pwd", workDir);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        instance.execute(out);
        String actual = new String(out.toByteArray()).trim();
        if (actual.startsWith("/private")) {
            actual = StringUtils.substringAfter(actual, "/private");
        }
        assertEquals(workDir.getAbsolutePath(), actual);
    }
}

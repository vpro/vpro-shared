package nl.vpro.util;

import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.LoggerWrapper;

import nl.vpro.logging.LoggerOutputStream;

/**
 * Wrapper around ProcessorBuilder
 * It makes calling that somewhat simpler and also implements an interface, for easier mocking in test cases.
 * It supports a timeout, for implicit killing the process if it takes too long. It also can wrap stderr to log that as errors.
 *
 * @author Michiel Meeuwissen
 * @since 1.6
 */
public class CommandExecutorImpl implements CommandExecutor {

    private final String binary;

    private final File workdir;

    private long processTimeout = -1L;
    private static final Timer PROCESS_MONITOR = new Timer(true); // create as daemon so that it shuts down at program exit

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    public CommandExecutorImpl(String c) {
        this(c, null);
    }

    public CommandExecutorImpl(File f) {
        this(f, (File) null);
    }

    public CommandExecutorImpl(String binary, File workdir) {
        if (workdir != null && !workdir.exists()) {
            throw new RuntimeException("Executable " + workdir.getAbsolutePath() + " does not exist.");
        }
        this.binary = binary;
        this.workdir = workdir;
    }

    public CommandExecutorImpl(File f, File workdir) {
        if (!f.exists()) {
            throw new RuntimeException("Executable " + f.getAbsolutePath() + " not found!");
        }
        if (!f.canExecute()) {
            throw new RuntimeException("Executable " + f.getAbsolutePath() + " is not executable!");
        }
        if (workdir != null && !workdir.exists()) {
            throw new RuntimeException("Working directory " + workdir.getAbsolutePath() + " does not exist.");
        }
        binary = f.getAbsolutePath();
        this.workdir = workdir;
    }

    @lombok.Builder(builderClassName = "Builder")
    private CommandExecutorImpl(
        File workdir,
        @Singular
        List<File> executables,
        Logger logger,
        Function<String, String> wrapLogInfo) {
         if (workdir != null && !workdir.exists()) {
            throw new RuntimeException("Working directory " + workdir.getAbsolutePath() + " does not exist.");
        }
        File f = null;
        for (File executable : executables) {
            if (executable.exists() && executable.canExecute()) {
                f = executable;
                break;
            }
        }
        if (f == null) {
            throw new RuntimeException("None of " + executables + "can be executed");
        }
        binary = f.getAbsolutePath();
        this.workdir = workdir;
        if (logger != null) {
            this.logger = logger;
        }
        if (wrapLogInfo != null) {
            this.logger = new LoggerWrapper(this.logger, this.logger.getName()) {
                @Override
                public void info(String message) {
                    super.info(wrapLogInfo.apply(message));
                }
            };
        }

    }

    public static class Builder {

    }
    @Override
    public int execute(String... args) {
        return execute(LoggerOutputStream.info(getLogger()), null, args);
    }

    @Override
    public int execute(final OutputStream out, OutputStream errors, String... args) {
        return execute(null, out, errors, args);
    }

    @Override
    public int execute(InputStream in, final OutputStream out, OutputStream errors, String... args) {
        if (errors == null) {
            errors = LoggerOutputStream.error(getLogger(), true);
        }
        final List<String> command = new ArrayList<>();
        command.add(binary);
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workdir != null) {
            pb.directory(workdir);
        }
        Process p;
        try {
            Collections.addAll(command, args);
            logger.info(toString(command));
            p = pb.start();

            final ProcessTimeoutHandle handle;
            if (processTimeout > 0L) {
                handle = startProcessTimeoutMonitor(p, "" + command, processTimeout * 1000);
            } else {
                handle = null;
            }
            Copier inputCopier = in != null ? copyThread(in, p.getOutputStream(), (e) -> {}) : null;

            Copier copier = out != null ? copyThread(p.getInputStream(), out, (e) -> {
                Process process = p.destroyForcibly();
                logger.info("Killed {}", process);
            }) : null;

            Copier errorCopier = copyThread(p.getErrorStream(), errors, (e) -> {});
            if (inputCopier != null) {
                inputCopier.waitFor();
                p.getOutputStream().close();
            }
            p.waitFor();
            if (copier != null) {
                copier.waitFor();
            }
            errorCopier.waitFor();
            int result = p.exitValue();
            if (result != 0) {
                logger.error("Error {} occurred while calling {}", result, command.stream().collect(Collectors.joining(" ")));
            }
            if (out != null) {
                out.flush();
            }
            errors.flush();
            if (handle != null) {
                handle.cancel();
            }
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            if (e.getCause() == null || ! e.getCause().getMessage().equalsIgnoreCase("broken pipe")) {
                logger.error(e.getMessage(), e);
            } else {
                logger.info(e.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stream<String> lines(InputStream in, OutputStream errors, String... args) {
        try {
            PipedInputStream reader = new PipedInputStream();
            PipedOutputStream writer = new PipedOutputStream(reader);

            BufferedReader result = new BufferedReader(new InputStreamReader(reader));
            submit(in, writer, errors, (i) -> {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ignored) {

                }
            }, args);
            return result.lines();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);

        }
    }

    public void setProcessTimeout(long processTimeout) {
        this.processTimeout = processTimeout;
    }

    Logger getLogger() {
        String[] split = binary.split("[\\/\\.\\\\]+");
        StringBuilder category = new StringBuilder(CommandExecutorImpl.class.getName());
        for (int i = split.length - 1; i >= 0; i--) {
            if (split[i].length() > 0) {
                category.append('.').append(split[i]);
            }
        }
        return LoggerFactory.getLogger(category.toString());
    }


    public static Copier copyThread(InputStream in, OutputStream out, Consumer<Throwable> errorHandler) {
        Copier copier = Copier.builder()
        .input(in)
        .output(out)
        .errorHandler((c, t) -> errorHandler.accept(t))
        .build();
        ThreadPools.copyExecutor.execute(copier);
        return copier;
    }


    public static String toString(Iterable<String> args) {
        StringBuilder builder = new StringBuilder();
        for (String a : args) {
            if (builder.length() > 0) builder.append(' ');
            boolean needsQuotes = a.indexOf(' ') >= 0 || a.indexOf('|') > 0;
            if (needsQuotes) builder.append('"');
            builder.append(a.replaceAll("\"", "\\\""));
            if (needsQuotes) builder.append('"');
        }
        return builder.toString();

    }

    private static class ProcessTimeoutHandle {
        private final ProcessTimeoutTask task;

        protected ProcessTimeoutHandle(ProcessTimeoutTask task) {
            this.task = task;
        }

        public void cancel() {
            task.cancel();
            PROCESS_MONITOR.purge();
        }

    }


    @Slf4j
    private static class ProcessTimeoutTask extends TimerTask {
        private final Process monitoredProcess;
        private final String command;

        protected ProcessTimeoutTask(Process monitoredProcess, String command) {
            this.monitoredProcess = monitoredProcess;
            this.command = command;
        }

        @Override
        public void run() {
            try {
                // already terminated? If not, it throws IllegalThreadStateException, otherwise it will
                // return forked process' exit value, which we aren't interested in
                monitoredProcess.exitValue();
            } catch (IllegalThreadStateException itse) {
                // wasn't terminated, kill it
                log.warn("The process {} took too long, killing it.", command);
                monitoredProcess.destroy();
            }
        }
    }


    private static ProcessTimeoutHandle startProcessTimeoutMonitor(Process process, String command, long timeout) {
        ProcessTimeoutTask task = new ProcessTimeoutTask(process, command); // task fires after timeout and kills the process
        PROCESS_MONITOR.schedule(task, timeout); // schedule the task to fire

        return new ProcessTimeoutHandle(task); // wrap the task so we can cancel when process finishes before timeout occurs
    }

    @Override
    public String toString() {
        return binary;
    }


}

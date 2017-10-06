package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;
import nl.vpro.logging.LoggerOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Wrapper around ProcessorBuilder
 * It makes calling that somewhat simpler and also implements an interface, for easier mocking in test cases.
 * It supports a timeout, for implicit killing the process if it takes too long. It also can wrap stderr to log that as errors.
 *
 * @author Michiel Meeuwissen
 * @since 1.6
 */
@Slf4j
public class CommandExecutorImpl implements CommandExecutor {

    private final String binary;

    private final File workdir;

    private long processTimeout = -1L;
    private static final Timer PROCESS_MONITOR = new Timer(true); // create as daemon so that it shuts down at program exit


    public CommandExecutorImpl(String c) {
        this(c, null);
    }

    public CommandExecutorImpl(File f) {
        this(f, null);
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
            log.info(toString(command));
            p = pb.start();

            final ProcessTimeoutHandle handle;
            if (processTimeout > 0L) {
                handle = startProcessTimeoutMonitor(p, "" + command, processTimeout * 1000);
            } else {
                handle = null;
            }
            Copier inputCopier = in != null ? copyThread(in, p.getOutputStream()) : null;

            Copier copier = out != null ? copyThread(p.getInputStream(), out) : null;
            Copier errorCopier = copyThread(p.getErrorStream(), errors);
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
                log.error("Error {} occurred while calling {}", result, command);
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
            log.error(e.getMessage(), e);
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
                } catch (IOException iae) {

                }
            }, args);
            return result.lines();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
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


    public static Copier copyThread(InputStream in, OutputStream out) {
        Copier copier = new Copier(in, out);
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

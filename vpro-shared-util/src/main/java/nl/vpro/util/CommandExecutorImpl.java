package nl.vpro.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.logging.simple.*;

/**
 * Wrapper around ProcessorBuilder
 * It makes calling that somewhat simpler and also implements an interface, for easier mocking in test cases.
 * It supports a timeout, for implicit killing the process if it takes too long. It also can wrap stderr to log that as errors.
 *
 * @author Michiel Meeuwissen
 * @since 1.6
 */
public class CommandExecutorImpl implements CommandExecutor {

    @Getter
    private final Supplier<String> binary;

    private final List<String> commonArgs;

    private final File workdir;

    private long processTimeout = -1L;
    private static final Timer PROCESS_MONITOR = new Timer(true); // create as daemon so that it shuts down at program exit

    private SimpleLogger logger;

    private boolean useFileCache = false;

    private int batchSize = 8192;

    private Function<CharSequence, String> wrapLogInfo = CharSequence::toString;

    private Boolean closeStreams = null;


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
        this.binary = () -> binary;
        this.workdir = workdir;
        this.commonArgs = null;
        this.logger = getDefaultLogger(binary);
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
        binary = f::getAbsolutePath;
        this.workdir = workdir;
        this.commonArgs = null;
        this.logger = getDefaultLogger(binary.get());
    }

    @lombok.Builder(builderClassName = "Builder", buildMethodName = "_build")
    private CommandExecutorImpl(
        File workdir,
        List<File> executables,
        Logger logger,
        SimpleLogger simpleLogger,
        Function<CharSequence, String> wrapLogInfo,
        List<String> commonArgs,
        boolean useFileCache,
        int batchSize,
        boolean optional,
        Boolean closeStreams
        ) {
         if (workdir != null && !workdir.exists()) {
            throw new RuntimeException("Working directory " + workdir.getAbsolutePath() + " does not exist.");
        }
        if (logger != null) {
            this.logger = new Slf4jSimpleLogger(logger);
        } else {
            this.logger = getDefaultLogger("");
        }
        Optional<File> f = getExecutable(executables);
        if (! f.isPresent()) {
            if (! optional) {
                throw new RuntimeException("None of " + executables + " can be executed");
            } else {
                this.logger.debug("None of {} can be executed", executables);
                binary = new Supplier<String>() {
                    @Override
                    public String get() {
                        return getExecutable(executables).map(File::getAbsolutePath).orElse(null);
                    }
                    @Override
                    public String toString() {
                        return "" + executables;
                    }
                };
            }
        } else {
            binary = () -> f.get().getAbsolutePath();
        }
        this.workdir = workdir;
        if (logger != null) {
            this.logger = new Slf4jSimpleLogger(logger);
        } else {
            this.logger = getDefaultLogger(binary.get());
        }
        if (wrapLogInfo != null) {
            this.logger = new SimpleLoggerWrapper(this.logger) {
                @Override
                protected String wrapMessage(CharSequence message) {
                    return wrapLogInfo.apply(message);

                }
            };
            this.wrapLogInfo = wrapLogInfo;

        }
        if (simpleLogger != null) {
            this.logger = this.logger.chain(simpleLogger);

        }
        this.commonArgs = commonArgs;
        this.useFileCache = useFileCache;
        this.batchSize = batchSize;
        this.closeStreams = closeStreams;

    }

    public static Optional<File> getExecutable(Collection<File> proposals) {
        return proposals.stream().filter((e) -> e.exists() && e.canExecute()).findFirst();
    }

    public static Optional<File> getExecutable(String... proposals) {
        return Arrays.stream(proposals).map(File::new).filter((e) -> e.exists() && e.canExecute()).findFirst();
    }

      public static Optional<File> getExecutableFromStrings(Collection<String> proposals) {
        return proposals.stream().map(File::new).filter((e) -> e.exists() && e.canExecute()).findFirst();
    }

    public static class Builder {

        private List<String> cargs = new ArrayList<>();
        private List<File>   execs = new ArrayList<>();

        public Builder commonArg(String... args) {
            cargs.addAll(Arrays.asList(args));
            return this;
        }
        public Builder executable(File... args) {
            execs.addAll(Arrays.asList(args));
            return this;
        }

        public Builder executablesPaths(String... executables) {
            return executablesPaths(Arrays.asList(executables));
        }

        public Builder executablesPaths(Iterable<String> executables) {
            for (String executable : executables) {
                executable(new File(executable));
            }
            return this;
        }
        public Builder executablesPath(String executable) {
            return executablesPaths(executable);
        }

        public CommandExecutorImpl build() {
            if (commonArgs != null) {
                commonArgs = new ArrayList<>(commonArgs);
                commonArgs.addAll(cargs);
            } else {
                commonArgs(cargs);
            }
            if (executables != null) {
                executables = new ArrayList<>(executables);
                executables.addAll(execs);
            } else {
                executables(execs);
            }
            return _build();
        }

    }

    /**
     * Eecutes the command with given arguments. Output is logged only.
     * @return  The exit code
     */
    @Override
    public int execute(String... args) {
        return execute(LoggerOutputStream.info(getLogger()), null, args);
    }


    /**
     * Executes the command with given arguments, catch the output and error streams in the given output streams, and provide standard input with the given input stream
     * @return  The exit code
     */
    @Override
    public int execute(InputStream in, final OutputStream out, OutputStream errors, String... args) {

        if (errors == null) {
            errors = LoggerOutputStream.error(getLogger(), true);
        }

        final List<String> command = new ArrayList<>();
        String b = binary.get();
        if (b == null) {
            throw new IllegalStateException("No binary found");
        }
        command.add(binary.get());
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workdir != null) {
            pb.directory(workdir);
        }
        Process p;
        try {
            if (commonArgs != null) {
                command.addAll(commonArgs);
            }
            Collections.addAll(command, args);
            logger.info(toString(command));
            p = pb.start();

            final ProcessTimeoutHandle handle;
            if (processTimeout > 0L) {
                handle = startProcessTimeoutMonitor(p, "" + command, processTimeout * 1000);
            } else {
                handle = null;
            }
            final Copier inputCopier = in != null ? copyThread("input copier", in, p.getOutputStream(), (e) -> {}) : null;

            final Copier copier;
            if (out != null) {
                InputStream commandOutput = p.getInputStream();
                if (useFileCache) {
                    commandOutput = FileCachingInputStream
                        .builder()
                        .input(commandOutput)
                        .noProgressLogging()
                        .build();
                }
                copier = copyThread("command output", commandOutput, out,
                    (e) -> {
                    Process process = p.destroyForcibly();
                    logger.info("Killed {} because {}: {}", process, e.getClass(), e.getMessage());
                });
            } else {
                copier = null;
            }

            Copier errorCopier = copyThread("error copier", p.getErrorStream(), errors, (e) -> {});
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
                logger.error("Error {} occurred while calling {}", result, String.join(" ", command));
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
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            if (CommandExecutor.isBrokenPipe(e)) {
                logger.debug(e.getMessage());
                throw new BrokenPipe(e);
            } else {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);

            }

        }
    }



    public void setProcessTimeout(long processTimeout) {
        this.processTimeout = processTimeout;
    }

    @Override
    public SimpleLogger getLogger() {
        return logger;
    }

    private static SimpleLogger getDefaultLogger(String binary) {
        String[] split;
        if (binary == null) {
            split = new String[0];
        } else {
            split = binary.split("[/.\\\\]+");
        }
        StringBuilder category = new StringBuilder(CommandExecutorImpl.class.getName());
        for (int i = split.length - 1; i >= 0; i--) {
            if (split[i].length() > 0) {
                category.append('.').append(split[i]);
            }
        }
        return Slf4jSimpleLogger.of(category.toString());
    }

    Copier copyThread(String name, InputStream in, OutputStream out, Consumer<Throwable> errorHandler) {
        Copier copier = Copier.builder()
            .name(name)
            .input(in)
            .output(out)
            .callback((c) -> {
                closeIf(in, out);
            })
            .errorHandler((c, t) -> errorHandler.accept(t))
            .batch(batchSize)
            .build();
        copier.execute();
        return copier;
    }

    private boolean needsClose(Closeable closeable) {
        if (this.closeStreams != null) {
            return this.closeStreams;
        }
        return closeable != System.out && closeable != System.in && closeable != System.err;
    }
    private void closeIf(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (needsClose(closeable)) {
                try {
                    closeable.close();
                } catch (IOException ioe) {
                    logger.warn(ioe.getMessage());
                }
            }
        }
    }


    static String toString(Iterable<String> args) {
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
        return binary + (commonArgs == null ? "" : commonArgs.stream()
            .map(s -> this.wrapLogInfo.apply(s))
            .collect(Collectors.joining(" ")));
    }


}

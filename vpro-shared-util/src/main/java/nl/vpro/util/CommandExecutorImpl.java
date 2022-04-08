package nl.vpro.util;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.logging.simple.*;

import static nl.vpro.util.CommandExecutor.isBrokenPipe;
import static org.meeuw.functional.Functions.ignoreArg1;
import static org.meeuw.functional.Functions.withArg1;

/**
 * Wrapper around ProcessorBuilder
 * It makes calling that somewhat simpler and also implements an interface, for easier mocking in test cases.
 * It supports a timeout, for implicit killing the process if it takes too long. It also can wrap stderr to log that as errors.
 *
 * @author Michiel Meeuwissen
 * @since 1.6
 */
public class CommandExecutorImpl implements CommandExecutor {

    private static final Timer PROCESS_MONITOR = new Timer(true); // create as daemon so that it shuts down at program exit
    private static final int DEFAULT_BATCH_SIZE = 8192;
    private static final Function<Integer, Level> DEFAULT_EXIT_CODE_LEVEL = (exitCode) -> {
        if (exitCode == null) exitCode = -1;
        switch(exitCode) {
            case 0:
                return Level.DEBUG;
            case 137:
                return Level.WARN;
            default:
                return Level.ERROR;
        }
    };

    @Getter
    private final Supplier<String> binary;

    private final List<Supplier<String>> commonArgs;

    private final File workdir;

    private final Duration processTimeout;

    private final SimpleLogger logger;

    private final boolean useFileCache;

    private final int batchSize;

    private final BiFunction<Level, CharSequence, String> wrapLogInfo;

    private final Boolean closeStreams;

    private final Function<Integer, Level> exitCodeLogLevel;


    public CommandExecutorImpl(String c) {
        this(c, null);
    }

    public CommandExecutorImpl(File f) {
        this(f, null);
    }

    public CommandExecutorImpl(String binary, File workdir) {
        this(binary, workdir, null);
    }

    public CommandExecutorImpl(String binary, File workdir, Duration processTimeout) {
        this.workdir = getWorkdir(workdir);
        this.binary = () -> binary;
        this.commonArgs = null;
        this.logger = getDefaultLogger(this.binary.get());
        this.processTimeout = processTimeout;
        this.wrapLogInfo = ignoreArg1(CharSequence::toString);
        this.closeStreams = null;
        this.batchSize = DEFAULT_BATCH_SIZE;
        this.useFileCache = false;
        this.exitCodeLogLevel = DEFAULT_EXIT_CODE_LEVEL;
    }

    public CommandExecutorImpl(File f, File workdir) {
        this(f.getAbsolutePath(), workdir);
        if (!f.exists()) {
            throw new IllegalArgumentException("Executable " + f.getAbsolutePath() + " not found!");
        }
        if (f.isDirectory()) {
            throw new IllegalArgumentException("Executable " + f.getAbsolutePath() + " is a directory");
        }
        if (! f.canExecute()) {
            throw new IllegalArgumentException("Executable " + f.getAbsolutePath() + " is a directory");
        }
    }

    @lombok.Builder(builderClassName = "Builder", buildMethodName = "_build")
    private CommandExecutorImpl(
        File workdir,
        List<File> executables,
        Logger logger,
        SimpleLogger simpleLogger,
        BiFunction<Level, CharSequence, String> biWrapLogInfo,
        @Singular List<Object> commonArgsSuppliers,
        boolean useFileCache,
        Integer batchSize,
        boolean optional,
        Boolean closeStreams,
        Duration processTimeout,
        Function<Integer, Level> exitCodeLogLevel
        ) {
        this.workdir = getWorkdir(workdir);
        this.wrapLogInfo =  biWrapLogInfo == null  ? ignoreArg1(CharSequence::toString) : biWrapLogInfo;
        this.binary = getBinary(executables, optional);
        this.logger = assembleLogger(logger, simpleLogger, wrapLogInfo);
        this.commonArgs = (commonArgsSuppliers == null ? Stream.empty() : commonArgsSuppliers.stream())
            .map(this::toString)
            .collect(Collectors.toList());
        this.useFileCache = useFileCache;
        this.batchSize = batchSize == null ? DEFAULT_BATCH_SIZE : batchSize;
        this.closeStreams = closeStreams;
        this.processTimeout = processTimeout;
        this.exitCodeLogLevel = exitCodeLogLevel == null ? DEFAULT_EXIT_CODE_LEVEL : exitCodeLogLevel;

    }

    private Supplier<String> toString(Object o) {
        if (o instanceof Supplier) {
            return () -> toString(((Supplier<?>) o).get()).get();
        } else {
            return () -> o == null ? null : String.valueOf(o);
        }
    }

    private SimpleLogger assembleLogger(Logger logger, SimpleLogger simpleLogger, BiFunction<Level, CharSequence, String> wrapLogInfo) {
        SimpleLogger result;
        if (logger != null) {
            result = new Slf4jSimpleLogger(logger);
        } else {
            result = getDefaultLogger(this.binary.get());
        }
        if (simpleLogger != null) {
            result = result.chain(simpleLogger);
        }
        if (wrapLogInfo != null) {
            result = new SimpleLoggerWrapper(result) {
                @Override
                protected String wrapMessage(nl.vpro.logging.simple.Level level, CharSequence message) {
                    return wrapLogInfo.apply(level, message);

                }
            };
        }
        return result;
    }

    private Supplier<String> getBinary(List<File> executables, boolean optional) {
        Optional<File> f = getExecutable(executables);
        if (! f.isPresent()) {
            if (! optional) {
                throw new RuntimeException("None of " + executables + " can be executed");
            } else {
                //log.debug("None of {} can be executed", executables);
                return new Supplier<String>() {
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
            return () -> f.get().getAbsolutePath();
        }

    }
    private static File getWorkdir(File workdir) {
        if (workdir != null && !workdir.exists()) {
            throw new IllegalArgumentException("Working directory " + workdir.getAbsolutePath() + " does not exist.");
        }
        return workdir;

    }


    public static Optional<File> getExecutable(Collection<File> proposals) {
        return proposals.stream().filter((e) -> e.exists() && e.canExecute()).findFirst();
    }

    public static Optional<File> getExecutable(String... proposals) {
        return Arrays.stream(proposals).map(File::new).filter((e) -> e.exists() && e.isFile() && e.canExecute()).findFirst();
    }

    public static Optional<File> getExecutableFromStrings(Collection<String> proposals) {
        return proposals.stream().map(File::new).filter((e) -> e.exists() && e.isFile() && e.canExecute()).findFirst();
    }


    public static class Builder {

        private final List<File>   execs = new ArrayList<>();

        public Builder commonArgs(List<String> args) {
            for (String s : args) {
                commonArgsSupplier(s);
            }
            return this;
        }

        public Builder commonArg(String... args) {
            for (String s : args) {
                commonArgsSupplier(s);
            }
            return this;
        }

        @SafeVarargs
        public final Builder commonArg(Supplier<Object>... args) {
            for (Supplier<Object> s : args) {
                commonArgsSupplier(s);
            }
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

        public Builder wrapLogInfo(Function<CharSequence, String> wrapLoginfo) {
            return wrapLogInfo(ignoreArg1(wrapLoginfo));
        }
        public Builder wrapLogInfo(BiFunction<Level, CharSequence, String> wrapLoginfo) {
            return biWrapLogInfo(wrapLoginfo);
        }

        public CommandExecutorImpl build() {
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
    @SneakyThrows
    public int execute(String... args) {
        try (OutputStream out = LoggerOutputStream.info(getLogger())) {
            return execute(out, null, args);
        }
    }


    /**
     * Executes the command with given arguments, catch the output and error streams in the given output streams, and provide standard input with the given input stream
     * @return  The exit code
     */
    @Override
    public int execute(Parameters parameters) {

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
                command.addAll(commonArgs.stream().map(Supplier::get).collect(Collectors.toList()));
            }
            Collections.addAll(command, parameters.args);
            logger.info(toString(command));
            p = pb.start();
            parameters.consumer.accept(p);

            final ProcessTimeoutHandle handle;
            if (processTimeout != null) {
                handle = startProcessTimeoutMonitor(p, "" + command, processTimeout);
            } else {
                handle = null;
            }
            final Copier inputCopier = parameters.in != null ?
                copyThread(
                    "input -> process input copier",
                    parameters.in,
                    p.getOutputStream(),
                    (c) -> {
                        closeSilently(p.getOutputStream());
                    },
                    (e) -> {},
                    p
                ) : null;

            final Copier copier;
            if (parameters.out != null) {
                InputStream commandOutput;
                if (useFileCache) {
                    commandOutput = FileCachingInputStream
                        .builder()
                        .input(p.getInputStream())
                        .noProgressLogging()
                        .build();
                } else {
                    commandOutput = p.getInputStream();
                }
                copier = copyThread("process output parameters out copier",
                    commandOutput,
                    parameters.out,
                    (c) -> {
                        closeSilently(commandOutput);
                    },
                    (e) -> {
                        Process process = p.destroyForcibly();
                        logger.info("Killed {} because {}: {}", process, e.getClass(), e.getMessage());
                    }, p);
            } else {
                copier = null;
            }

            Copier errorCopier = copyThread(
                "error copier",
                p.getErrorStream(),
                parameters.errors,
                (c) -> {
                    closeSilently(p.getErrorStream());
                },
                (e) -> {},
                p
            );
            if (inputCopier != null) {
                if (needsClose(p.getInputStream())) {
                    inputCopier.waitForAndClose();
                } else {
                    inputCopier.waitFor();
                }
            }

            p.waitFor();

            if (copier != null) {
                copier.waitForAndClose();
            }
            errorCopier.waitForAndClose();
            int result = p.exitValue();
            if (result != 0) {
                logger.log(exitCodeLogLevel.apply(result), "Exit code {} for calling {}", result, String.join(" ", command));
            }
            if (parameters.out != null) {
                parameters.out.flush();
            }
            parameters.errors.flush();
            if (handle != null) {
                handle.cancel();
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            if (isBrokenPipe(e)) {
                logger.debug(e.getMessage());
                throw new BrokenPipe(e);
            } else {
                logger.error(e.getClass().getName() + ":" + e.getMessage(), e);
                throw new RuntimeException(e);

            }
        } finally {
            this.logger.debug("Ready");
        }
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

    Copier copyThread(
        String name,
        InputStream in,
        OutputStream out,
        Consumer<Copier> callBack,
        Consumer<Throwable> errorHandler,
        Process process) {
        Copier copier = Copier.builder()
            .name(name)
            .input(in)
            .output(out)
            .callback(callBack)
            .errorHandler((c, t) ->
                errorHandler.accept(t)
            )
            .notify(process)
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

    private boolean closeIf(Closeable... closeables) {
        boolean value = false;
        for (Closeable closeable : closeables) {
            if (needsClose(closeable)) {
                value = closeSilently(closeable);
            } else {
                logger.debug("Not closing {}", closeables);
            }
        }
        return value;
    }

    private boolean closeSilently(Closeable closeable) {
        try {
            logger.debug("Closing {}", closeable);
            closeable.close();
            return true;
        } catch (IOException ioe) {
            logger.warn(ioe.getClass().getName() + ":" + ioe.getMessage());
            return false;
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


    private static ProcessTimeoutHandle startProcessTimeoutMonitor(
        Process process, String command, Duration timeout) {
        ProcessTimeoutTask task = new ProcessTimeoutTask(process, command); // task fires after timeout and kills the process
        PROCESS_MONITOR.schedule(task, timeout.toMillis()); // schedule the task to fire

        return new ProcessTimeoutHandle(task); // wrap the task so we can cancel when process finishes before timeout occurs
    }

    @Override
    public String toString() {
        return binary.get() + (commonArgs == null ? "" : " " +
            commonArgs.stream()
                .map(Supplier::get)
                .map(withArg1(this.wrapLogInfo, Level.INFO))
                .collect(Collectors.joining(" ")));
    }


}

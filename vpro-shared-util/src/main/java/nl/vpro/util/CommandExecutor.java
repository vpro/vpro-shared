package nl.vpro.util;

import lombok.Getter;
import lombok.Singular;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.LoggerFactory;

import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.logging.simple.SimpleLogger;

/**
 * Executor for external commands.
 *
 * Three types of methods:
 *<ul>
 * <li>{@link #execute(InputStream, OutputStream, OutputStream, String...)} To synchronously execute and return exit code.</li>
 * <li>{@link #submit(InputStream, OutputStream, OutputStream, Consumer, String...)} For asynchronous execution</li>
 * </ul>
 * These  two also have versions with a {@link Parameters} argument, so you can use builder pattern to fill in parameters.
 *<ul>
 * <li>{@link #lines(InputStream, OutputStream, String...)} For synchronous execution and returing the output as a stream of strings.</li>
 *</ul>
 * @author Michiel Meeuwissen
 * @since 1.6
 */
public interface CommandExecutor {


    /**
     * Executes with the given arguments. The command itself is supposed to be a member of the implementation, so you
     * would have a CommandExecutor instance for every external program you'd like to wrap.
     * The version with no outputstream argument logs the output.
     * @return the exit code
     */
    int execute(String... args);

    /**
     * Executes the command
     *
     * @param out Stdout of the command will be written to this.
     * @param errors Stderr of the command will be written to this. To log errors use {@link nl.vpro.logging.LoggerOutputStream#error(org.slf4j.Logger)}
     * @param args The command and its arguments to be executed on the remote server
     * @return The exit code
     */
    default int execute(OutputStream out, OutputStream errors, String... args) {
        return execute(null, out, errors, args);

    }

    /**
     * Executes the command with given arguments. Stderr will be logged via slf4j
     * @param out Stdout will be written to this.
     * @return the exit code
     */
    default int execute(OutputStream out, String... args) {
        return execute(out, LoggerOutputStream.error(LoggerFactory.getLogger(getClass())), args);
    }

    default int execute(Writer out, String... args) {
        return execute(new WriterOutputStream(out, StandardCharsets.UTF_8), LoggerOutputStream.error(LoggerFactory.getLogger(getClass())), args);
    }


    /**
     * Executes the command
     *
     * @param in  Stdin of the command will be taken from this.
     * @param out Stdout of the command will be written to this.
     * @param error Stder of the command will be written to this.
     * @return The exit code
     */
    default int execute(InputStream in, OutputStream out, OutputStream error, String... args) {
        return execute(Parameters.builder()
            .in(in)
            .out(out)
            .errors(error)
            .args(args)
            .build());
    }


    int execute(Parameters parameters);

    default int execute(Parameters.Builder parameters) {
        return execute(parameters.build());
    }

   /**
    * Executes the command in the background.
    * @param callback will be called when ready.
    * @return A future producing the result code.
    */
    default CompletableFuture<Integer> submit(InputStream in, OutputStream out, OutputStream error, IntConsumer callback, String... args) {
        return submit(callback, Parameters.builder()
            .in(in)
            .out(out)
            .errors(error)
            .args(args)
            .build());

    }
    default CompletableFuture<Integer> submit(IntConsumer callback, Parameters.Builder parameters) {
        return submit(callback, parameters.build());
    }


    default CompletableFuture<Integer> submit(IntConsumer callback, Parameters parameters) {
        return CompletableFuture.supplyAsync(() -> {
            int result = -1;
            try {
                result = execute(parameters);
                return result;
            } finally {
                if (callback != null) {
                    synchronized (callback) {
                        callback.accept(result);
                        callback.notifyAll();
                    }
                }
            }
        });
    }

    default CompletableFuture<Integer> submit(InputStream in, OutputStream out, OutputStream error, String... args) {
        return submit(in, out, error, null, args);
    }

    default CompletableFuture<Integer> submit(OutputStream out, OutputStream error, String... args) {
        return submit(null, out, error, args);
    }

     default CompletableFuture<Integer> submit(OutputStream out, String... args) {
        return submit(out, LoggerOutputStream.error(LoggerFactory.getLogger(getClass())), args);
    }

    default CompletableFuture<Integer> submit(Consumer<Integer> callback, String... args) {
        return CompletableFuture.supplyAsync(() -> {
            Integer result = null;
            try {
                result = execute(args);
                return result;
            } finally {
                if (callback != null) {
                    callback.accept(result);
                }
            }
        });
    }

    default CompletableFuture<Integer> submit(String... args) {
        return submit((OutputStream) null, args);
    }


    /**
     * Executes the command streamingly. Stdout is converted to a stream of string (one for each line).
     * E.g.
     * <code>*
     *      CommandExecutorImpl env = new CommandExecutorImpl("/usr/bin/env");
     *      long running = env.lines("ps", "u").filter(s -> s.contains("amara_poms_publisher")).count();
     * </code>

     * @return  The exit code
     */
    default Stream<String> lines(InputStream in, OutputStream errors, String... args) {
        try {
            PipedInputStream reader = new PipedInputStream();
            PipedOutputStream writer = new PipedOutputStream(reader);
            BufferedReader result = new BufferedReader(new InputStreamReader(reader));


            CompletableFuture<Integer> submit = submit(in, writer, errors, (i) -> {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ignored) {
                }

            }, args);
            submit.whenComplete((i, t) -> {
                if (t != null) {
                    getLogger().error(t.getMessage());
                }
                getLogger().debug("Ready with {}", i);
            });
            return result.lines();
        } catch (IOException e) {
            getLogger().error(e.getMessage(), e);
            throw new RuntimeException(e);

        }
    }


    default Stream<String> lines(String... args) {
        return lines(null, LoggerOutputStream.error(LoggerFactory.getLogger(getClass())), args);
    }

    static boolean isBrokenPipe(Throwable ioe) {
         return ioe.getCause() != null && ioe.getCause().getMessage().equalsIgnoreCase("broken pipe");
    }

    SimpleLogger getLogger();


    class BrokenPipe extends RuntimeException {

        public BrokenPipe(IOException e) {
            super(e);
        }
    }


    @Getter
    class ExitCodeException extends RuntimeException {
        final int exitCode;
        public ExitCodeException(String message, int exitCode) {
            super(message);
            this.exitCode = exitCode;
        }

        @Override
        public String toString() {
            return super.toString() + " exitcode: " + exitCode;
        }
    }

    /**
     * The parameters of {@link #submit(IntConsumer, Parameters)}.
     */
    class Parameters {
        /**
         * InputStream (optional)
         */
        final InputStream in;
        final OutputStream out;
        final OutputStream errors;
        final Consumer<Process> consumer;
        final String[] args;

        @lombok.Builder(builderClassName = "Builder")
        public Parameters(InputStream in, OutputStream out, OutputStream errors, Consumer<Process> consumer, @Singular List<String> listArgs) {
            this.in = in;
            this.out = out;
            this.errors = errors == null ?
                LoggerOutputStream.error(LoggerFactory.getLogger(getClass()), true) : errors;
            this.consumer = consumer == null ? (p) -> {} : consumer;
            this.args = listArgs == null ? new String[0] : listArgs.toArray(new String[0]);
        }
        public static class Builder {

            public Builder args(String... args) {
                return listArgs(Arrays.asList(args));
            }

            public Builder arg(String... args) {
                for (String a : args) {
                    listArg(a);
                }
                return this;
            }

            public CompletableFuture<Integer> submit(IntConsumer exitCode, CommandExecutor executor) {
                return executor.submit(exitCode, this);

            }
            public CompletableFuture<Integer> submit(CommandExecutor executor) {
                return submit((exitCode) -> {}, executor);

            }
            public int execute(CommandExecutor executor) {
                return executor.execute(this);

            }

        }
    }
}

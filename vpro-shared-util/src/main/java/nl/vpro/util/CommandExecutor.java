package nl.vpro.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.LoggerFactory;

import nl.vpro.logging.LoggerOutputStream;

/**
 * Executor for external commands.
 *
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
     * @param error Stderr of the command will be written to this. To log errors use {@link nl.vpro.logging.LoggerOutputStream#error(org.slf4j.Logger)}
     * @param args The command and its arguments to be executed on the remote server
     * @return The exit code
     */
    int execute(OutputStream out, OutputStream error, String... args);

    /**
     * Executes the command with given arguments. Stderr will be logged via slf4j
     * @param out Stdout will be written to this.
     * @return the exit code
     */
    default int execute(OutputStream out, String... args) {
        return execute(out, LoggerOutputStream.error(LoggerFactory.getLogger(getClass())), args);
    }

    default int execute(Writer out, String... args) {
        return execute(new WriterOutputStream(out, "UTF-8"), LoggerOutputStream.error(LoggerFactory.getLogger(getClass())), args);
    }


    /**
     * Executes the command
     *
     * @param in  Stdin of the command will be taken from this.
     * @param out Stdout of the command will be written to this.
     * @param error Stder of the command will be written to this.
     * @return The exit code
     */
    int execute(InputStream in, OutputStream out, OutputStream error, String... args);


   /**
    * Executes the command in the background.
    * @param callback will be called when ready.
    * @return A future producing the result code.
    */
    default CompletableFuture<Integer> submit(InputStream in, OutputStream out, OutputStream error, Consumer<Integer> callback, String... args) {
        return CompletableFuture.supplyAsync(() -> {
            Integer result = null;
            try {
                result = execute(in, out, error, args);
                return result;
            } finally {
                if (callback != null) {
                    callback.accept(result);
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
     */
    Stream<String> lines(InputStream in, OutputStream errors, String... args);

    default Stream<String> lines(String... args) {
        return lines(null, LoggerOutputStream.error(LoggerFactory.getLogger(getClass())), args);
    }

}

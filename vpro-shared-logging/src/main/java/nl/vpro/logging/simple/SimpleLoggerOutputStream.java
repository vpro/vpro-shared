package nl.vpro.logging.simple;

import nl.vpro.logging.AbstractLoggerOutputStream;

/**
 * <p>
 * If you want to convert a {@link SimpleLogger} to a {@link java.io.OutputStream} this can be used.
 * Every line appearing on the output stream will be logged using the supplied {@link SimpleLogger}.
 *</p>
 * <p>
 * One use case is for example to convert lines output from some external command to events like so:
 * </p>
 * <pre>
 * {@code
 *         CommandExecutorImpl oc = CommandExecutorImpl.builder()
 *           .executablesPaths("/usr/local/bin/oc")
 *           .build();

 *         oc.submit(SimpleLoggerOutputStream.info(EventSimpleLogger.of(
 *             event -> {
 *                 if (event.getMessage().toString().contains("Forwarding from [::1]:" + jmxPort)) {
 *                     synchronized (container) {
 *                         container.ready = true;
 *                         container.notifyAll();
 *                     }
 *                 }
 *             })),
 *             "-n", "poms-stack-" + env, "port-forward", pod, jmxPort
 *         );
 * }
 * </pre>
 */
public abstract class SimpleLoggerOutputStream extends AbstractLoggerOutputStream {

    public SimpleLoggerOutputStream(boolean skipEmptyLines, Integer max) {
        super(skipEmptyLines, max);
    }

    public SimpleLoggerOutputStream(boolean skipEmptyLines) {
        super(skipEmptyLines, null);
    }

    public static SimpleLoggerOutputStream info(SimpleLogger log) {
        return info(log, false);
    }

    public static SimpleLoggerOutputStream info(SimpleLogger log, boolean skipEmptyLines) {
        return new SimpleLoggerOutputStream(skipEmptyLines) {
            @Override
            protected void log(String line) {
                log.info(line);
            }
        };
    }

}

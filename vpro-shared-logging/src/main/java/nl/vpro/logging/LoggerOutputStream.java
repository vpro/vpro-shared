package nl.vpro.logging;

import java.io.*;

import org.slf4j.Logger;

import nl.vpro.logging.simple.SimpleLogger;

/**
 * Wraps a {@link Logger} in an {@link OutputStream}, making logging available as an outputstream, which can be useful for things that accept outputstreams (e.g. external processes)
 *
 * Supports slf4j and JUL. For log4j2 see, {@link Log4j2OutputStream}
 *
 * @author Michiel Meeuwissen
 */
public abstract class LoggerOutputStream extends AbstractLoggerOutputStream {


    public static LoggerOutputStream info(java.util.logging.Logger log) {
        return info(log, false);
    }

    public static LoggerOutputStream info(final java.util.logging.Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.info(line);
            }
        };
    }

    public static LoggerOutputStream info(Logger log) {
        return info(log, false);
    }

    public static LoggerOutputStream info(final Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.info(line);
            }
        };
    }



    public static LoggerOutputStream info(SimpleLogger log) {
        return info(log, false);
    }

    public static LoggerOutputStream info(final SimpleLogger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            final void log(String line) {
                log.info(line);
            }
        };
    }

    public static LoggerOutputStream error(SimpleLogger log) {
        return error(log, false);
    }

    public static LoggerOutputStream error(SimpleLogger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            final void log(String line) {
                log.error(line);
            }
        };
    }


    public static LoggerOutputStream error(java.util.logging.Logger log) {
        return error(log, false);
    }

    public static LoggerOutputStream error(final java.util.logging.Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            final void log(String line) {
                log.severe(line);
            }
        };
    }


    public static LoggerOutputStream error(Logger log) {
        return error(log, false);
    }

    public static LoggerOutputStream error(final Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.error(line);
            }
        };
    }

    public static LoggerOutputStream warn(Logger log) {
        return warn(log, false, null);
    }

    public static LoggerOutputStream warn(java.util.logging.Logger log) {
        return warn(log, false);
    }

    public static LoggerOutputStream warn(final Logger log, boolean skipEmptyLines) {
        return warn(log, skipEmptyLines, null);
    }

    public static LoggerOutputStream warn(final Logger log, boolean skipEmptyLines, final Integer max) {
        return new LoggerOutputStream(skipEmptyLines, max) {
            @Override
            void log(String line) {

                log.warn(line);
            }
        };
    }

    public static LoggerOutputStream warn(final java.util.logging.Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.warning(line);
            }
        };
    }

	public static LoggerOutputStream debug(Logger log) {
		return debug(log, false);
	}

    public static LoggerOutputStream debug(java.util.logging.Logger log) {
        return debug(log, false);
    }



    public static LoggerOutputStream debug(final Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.debug(line);
            }
        };
    }

    public static LoggerOutputStream debug(final java.util.logging.Logger log, boolean skipEmptyLines) {
        return new LoggerOutputStream(skipEmptyLines) {
            @Override
            void log(String line) {
                log.fine(line);
            }
        };
    }


    LoggerOutputStream(boolean skipEmptyLines, Integer max) {
        super(skipEmptyLines, max);
    }
    LoggerOutputStream(boolean skipEmptyLines) {
        super(skipEmptyLines, null);
    }

}

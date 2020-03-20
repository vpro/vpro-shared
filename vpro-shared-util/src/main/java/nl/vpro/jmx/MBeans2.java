package nl.vpro.jmx;

import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.event.Level;

import nl.vpro.logging.simple.*;

;

/**
 * See {@link MBeans} but supporting log4j2.
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class MBeans2 {

     public static String returnMultilineString(
        @NonNull Logger log,
        @NonNull Consumer<StringSupplierSimpleLogger> logger) {
        return MBeans.returnString(multiLine(log), logger);
    }

    /**
     * @param log Logger instance to log too
     * @return a {@link StringBuilderSimpleLogger} representing multiple lines actually a {@link StringBuilderSimpleLogger}
     */
    public static StringSupplierSimpleLogger multiLine(Logger log) {
        return multiLine(log, null);
    }

     /**
     * @param log Logger instance to log too
     * @param message First line of the string (logged as info)
     * @param args The arguments of the first line
     * @return a {@link StringBuilderSimpleLogger} representing multiple lines actually a {@link StringBuilderSimpleLogger}
     */
    public static StringSupplierSimpleLogger multiLine(Logger log, String message, Object... args) {
        StringSupplierSimpleLogger string  = StringBuilderSimpleLogger.builder()
            .prefix((l) -> l.compareTo(Level.ERROR) < 0 ? "" : l.name() + " ")
            .chain(Log4j2SimpleLogger.of(log));
        if (message != null) {
            string.info(message, args);
        }
        return string;
    }

}

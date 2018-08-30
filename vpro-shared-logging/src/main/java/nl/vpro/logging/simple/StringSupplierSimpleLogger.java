package nl.vpro.logging.simple;

import java.util.function.Supplier;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public interface StringSupplierSimpleLogger extends SimpleLogger, Supplier<String> {
}

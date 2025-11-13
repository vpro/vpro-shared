package nl.vpro.logging.simple;

import java.util.function.Supplier;

/**
 * A {@link SimpleLogger} that is also a {@link Supplier} of {@link String}. The most straightforward implementation is {@link StringBuilderSimpleLogger}.
 *
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public interface StringSupplierSimpleLogger extends SimpleLogger, Supplier<String> {
}

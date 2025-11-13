package nl.vpro.logging.simple;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public  class StringSupplierChainedSimpleLogger extends ChainedSimpleLogger implements StringSupplierSimpleLogger {

    public StringSupplierChainedSimpleLogger(SimpleLogger... chained) {
        super(chained);
    }

    @Override
    public String get() {
        for (SimpleLogger l : list) {
            if (l instanceof StringSupplierSimpleLogger stringSupplierSimpleLogger) {
                return stringSupplierSimpleLogger.get();
            }
        }
        return null;
    }
}

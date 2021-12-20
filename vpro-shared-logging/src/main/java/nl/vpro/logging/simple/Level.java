package nl.vpro.logging.simple;

/**
 * @since 2.9
 */
public enum Level {

    ERROR(40),
    WARN(30),
    INFO(20),
    DEBUG(10),
    TRACE(0)
    ;
    private final int intValue;
    Level(int intValue) {
        this.intValue = intValue;
    }

    public int toInt() {
        return intValue;
    }
}

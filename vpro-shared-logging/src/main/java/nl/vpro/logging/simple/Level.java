package nl.vpro.logging.simple;

/**
 * @since 2.9
 */
public enum Level {

    TRACE(0),
    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40)
    ;
    private final int intValue;
    Level(int intValue) {
        this.intValue = intValue;
    }

    public int toInt() {
        return intValue;
    }
}

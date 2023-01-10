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

    private static int acceptableIndex(int i) {
        if (i < 0) {
            return 0;
        }
        if (i >= Level.values().length) {
            return Level.values().length -1;
        }
        return i;
    }
    public static Level shiftedLevel(Level l, int shift) {
        return values()[acceptableIndex(l.ordinal() + shift)];
    }
}

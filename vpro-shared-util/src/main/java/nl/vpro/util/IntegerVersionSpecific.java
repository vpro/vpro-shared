package nl.vpro.util;

/**
 * Classes which can behave differently according the specified version of the application can implement this.
 *
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public interface  IntegerVersionSpecific extends VersionSpecific<Integer> {

    /**
     * For which version this object is supposed to be filled.
     */
    @Override
    IntegerVersion getVersion();

    void setVersion(IntegerVersion version);
}

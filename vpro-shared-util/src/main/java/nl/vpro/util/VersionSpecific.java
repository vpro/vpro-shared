package nl.vpro.util;

/**
 * Classes which can behave differently according the specified version of the application can implement this.
 * @author Michiel Meeuwissen
 * @since 2.3
 */
public interface VersionSpecific<T extends Comparable<T>> {


    /**
     * For which version this object is supposed to be filled.
     */
    Version<T> getVersion();

}

package nl.vpro.api.client.utils;

import lombok.Getter;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Getter
public class VersionResult {


    private final String version;
    private final boolean available;

    @lombok.Builder
    public VersionResult(String version, boolean available) {
        this.version = version;
        this.available = available;
    }

}

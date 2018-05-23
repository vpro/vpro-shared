package nl.vpro.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * An abstract representation of a File, it can e.g. also be a file on a different server.
 * @author Michiel Meeuwissen
 * @since 1.78
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(builderClassName = "Builder")
public class FileMetadata {

    private final Long size;
    private final Instant lastModified;
    private final String fileName;

    @Override
    public String toString() {
        return fileName + " (" + size + " bytes) " + lastModified;
    }
}

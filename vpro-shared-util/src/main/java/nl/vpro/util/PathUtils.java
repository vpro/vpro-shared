package nl.vpro.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Utils related to {@link java.nio}
 *
 * @since 3.1
 */
public class PathUtils {


    private static Set<Path> paths = new LinkedHashSet<>();

    static {
        Runtime.getRuntime().addShutdownHook(
            new Thread(PathUtils::shutdownHook));
    }

    /**
     * Shutdown hook for {@link #deleteOnExit(Path)}
     */
    private static void shutdownHook() {
        Set<Path> local;
        synchronized(PathUtils.class) {
            local = paths;
            paths = null;
        }

        List<Path> toBeDeleted = new ArrayList<>(local);
        Collections.reverse(toBeDeleted);
        for (Path p : toBeDeleted) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException | RuntimeException e) {
                // do nothing - best-effort
            }
        }
    }


    /**
     * Very similar {@link File#deleteOnExit()} (code was more or less copied from there) but based on {@link Path}
     */
    public static synchronized void deleteOnExit(Path p) {
        if (paths == null) {
            throw new IllegalStateException("ShutdownHook already in progress.");
        }
        paths.add(p);
    }

}

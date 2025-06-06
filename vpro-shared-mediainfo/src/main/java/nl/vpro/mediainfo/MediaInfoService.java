package nl.vpro.mediainfo;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import jakarta.xml.bind.JAXB;

import org.slf4j.event.Level;

import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.util.CommandExecutor;
import nl.vpro.util.CommandExecutorImpl;

/**
 * Wrapper around the command line tools 'mediainfo'
 * @author Michiel Meeuwissen
 * @since 3.1
 */
@Slf4j
public class MediaInfoService implements Function<Path, MediaInfo> {

    private static final OutputStream STDERR = LoggerOutputStream
        .error(log, true);

    private final CommandExecutor mediainfo;

    public MediaInfoService(String... executablesPaths) {
         this(CommandExecutorImpl.builder()
             .executablesPaths(executablesPaths)
             .commonArg("-c", "--Output=XML")
             .build());
    }

    protected MediaInfoService(CommandExecutor mediainfo) {
         this.mediainfo = mediainfo;
    }

    public MediaInfoService() {
        this(
            "/usr/bin/mediainfo", // ubuntu
            "/opt/homebrew/bin/mediainfo"       // brew on macOS
        );
    }

    /**
     * Applies the mediainfo command to the given path, returning a {@link MediaInfo} object.
     *
     * @param path path to the file to get information about
     */
    @Override
    public MediaInfo apply(Path path) {
        assert Files.exists(path) : "File does not exist: " + path;
        assert Files.isRegularFile(path) : "File is not a regular file: " + path;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int status = mediainfo.execute(outputStream, STDERR, path.toAbsolutePath().toString());
        log.atLevel(status == 0 ? Level.DEBUG : Level.WARN).log("Mediainfo returned with status {}", status);


        return new MediaInfo(path,
            JAXB.unmarshal(new ByteArrayInputStream(outputStream.toByteArray()),
                net.mediaarea.mediainfo.MediaInfo.class),
            status
        );
    }


}

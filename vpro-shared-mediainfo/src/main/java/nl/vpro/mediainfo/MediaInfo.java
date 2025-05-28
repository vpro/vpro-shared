package nl.vpro.mediainfo;

import lombok.extern.slf4j.Slf4j;
import net.mediaarea.mediainfo.TrackType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import jakarta.xml.bind.JAXB;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.event.Level;

import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.util.CommandExecutor;
import nl.vpro.util.CommandExecutorImpl;

import static org.meeuw.math.IntegerUtils.gcd;

/**
 * Wrapper around the command line tools 'mediainfo'
 * @author Michiel Meeuwissen
 * @since 3.1
 */
@Slf4j
public class MediaInfo implements Function<Path, MediaInfo.Result> {

    private static final OutputStream STDERR = LoggerOutputStream
        .error(log, true);

    private final CommandExecutor mediainfo;

    public MediaInfo(String... executablesPaths) {
         this(CommandExecutorImpl.builder()
             .executablesPaths(executablesPaths)
             .commonArg("-c", "--Output=XML")
             .build());
    }

    protected MediaInfo(CommandExecutor mediainfo) {
         this.mediainfo = mediainfo;
    }

    public MediaInfo() {
        this(
            "/usr/bin/mediainfo", // ubuntu
            "/opt/homebrew/bin/mediainfo"       // brew on macOS
        );
    }

    /**
     *
     */
    @Override
    public Result apply(Path path) {
        assert Files.exists(path) : "File does not exist: " + path;
        assert Files.isRegularFile(path) : "File is not a regular file: " + path;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int status = mediainfo.execute(outputStream, STDERR, path.toAbsolutePath().toString());
        log.atLevel(status == 0 ? Level.DEBUG : Level.WARN).log("Mediainfo returned with status {}", status);


        return new Result(path, JAXB.unmarshal(new ByteArrayInputStream(outputStream.toByteArray()), net.mediaarea.mediainfo.MediaInfo.class), status);
    }

    /**
     * @param path the path to the file to get information about
     * @param mediaInfo The unmarshalled result of the call to {@code mediainfo}
     * @param status the exit status of the mediainfo command (0 for success, non-zero for failure)
     */
    public record Result(Path path, net.mediaarea.mediainfo.MediaInfo mediaInfo, int status) {

        Optional<TrackType> video() {
            return mediaInfo.getMedias().stream()
                .flatMap(m -> m.getTracks().stream())
                .filter(t -> "Video".equals(t.getTrackType()))
                .findFirst();
        }
        Optional<String> displayAspectRatio() {
            return video().map(MediaInfo::getAspectRatio);
        }

        public boolean success() {
            return status == 0;
        }
        public boolean vertical() {
            return video().map(t -> t.getHeight().intValue() > t.getWidth().intValue()).orElse(false);
        }

        @Override
        public @NonNull String toString() {
            return (success() ? "" : "FAIL:") + path() + (video().isPresent() ? " (video " + displayAspectRatio().get() + ")" :  " (no video track)");
        }
    }

    public static String getAspectRatio(TrackType trackType) {
        int height = trackType.getHeight().intValue();
        int width = trackType.getWidth().intValue();
        int ggcd = gcd(height, width);
        return String.format("%d:%d", height / ggcd, width / ggcd);
    }

}

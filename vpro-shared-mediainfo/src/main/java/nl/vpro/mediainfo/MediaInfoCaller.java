package nl.vpro.mediainfo;

import lombok.extern.slf4j.Slf4j;
import net.mediaarea.mediainfo.MediaInfo;
import net.mediaarea.mediainfo.TrackType;

import java.io.*;
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
public class MediaInfoCaller implements Function<Path, MediaInfoCaller.Result> {

    private static final OutputStream STDERR = LoggerOutputStream
        .error(log, true);

    private final CommandExecutor mediainfo;

    public MediaInfoCaller(String... executablesPaths) {
         this(CommandExecutorImpl.builder()
             .executablesPaths(executablesPaths)
             .commonArg("-c", "--Output=XML")
             .build());
    }

    protected MediaInfoCaller(CommandExecutor mediainfo) {
         this.mediainfo = mediainfo;
    }

    public MediaInfoCaller() {
        this(
            "/usr/bin/mediainfo", // ubuntu
            "/opt/homebrew/bin/mediainfo"       // brew on macOS
        );
    }

    @Override
    public Result apply(Path path) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int status = mediainfo.execute(outputStream, STDERR, path.toAbsolutePath().toString());
        log.atLevel(status == 0 ? Level.DEBUG : Level.WARN).log("Mediainfo returned with status {}", status);


        return new Result(path, JAXB.unmarshal(new ByteArrayInputStream(outputStream.toByteArray()), MediaInfo.class), status);
    }

    public record Result(Path path, MediaInfo mediaInfo, int status) {

        Optional<TrackType> video() {
            return mediaInfo.getMedias().stream()
                .flatMap(m -> m.getTracks().stream())
                .filter(t -> "Video".equals(t.getTrackType()))
                .findFirst();
        }
        Optional<String> displayAspectRatio() {
            return video().map(MediaInfoCaller::getAspectRatio);
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

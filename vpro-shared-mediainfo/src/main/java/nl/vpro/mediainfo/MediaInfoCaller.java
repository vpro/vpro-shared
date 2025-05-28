package nl.vpro.mediainfo;

import lombok.extern.slf4j.Slf4j;
import net.mediaarea.mediainfo.MediaInfo;
import net.mediaarea.mediainfo.TrackType;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import jakarta.xml.bind.JAXB;

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
        this( "/opt/homebrew/bin/mediainfo");
    }

    @Override
    public Result apply(Path path) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int status = mediainfo.execute(outputStream, STDERR, path.toAbsolutePath().toString());
        if (status != 0) {
            log.warn("Mediainfo returned with status " + status);
        }

        return new Result(JAXB.unmarshal(new ByteArrayInputStream(outputStream.toByteArray()), MediaInfo.class), status);
    }

    record Result(MediaInfo mediaInfo, int status) {

        Optional<TrackType> video() {
            return mediaInfo.getMedias().stream()
                .flatMap(m -> m.getTracks().stream())
                .filter(t -> "Video".equals(t.getTrackType()))
                .findFirst();
        }
        Optional<String> displayAspectRatio() {
            return video().map(MediaInfoCaller::getAspectRatio);
        }
    }

    public static String getAspectRatio(TrackType trackType) {
        int height = trackType.getHeight().intValue();
        int width = trackType.getWidth().intValue();
        int ggcd = gcd(height, width);
        return String.format("%d:%d", height / ggcd, width / ggcd);
    }

}

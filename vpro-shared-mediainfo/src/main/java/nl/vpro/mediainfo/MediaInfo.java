package nl.vpro.mediainfo;

import lombok.extern.slf4j.Slf4j;
import net.mediaarea.mediainfo.MediaType;
import net.mediaarea.mediainfo.TrackType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

import jakarta.xml.bind.JAXB;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
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
     * Applies the mediainfo command to the given path, returning a {@link Result} object.
     *
     * @param path path to the file to get information about
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
     * A wrapper around {@link net.mediaarea.mediainfo.MediaInfo} (which is the straightforwardly unmarshalled result of the mediainfo command).
     * @param path the path to the file to get information about
     * @param mediaInfo The unmarshalled result of the call to {@code mediainfo}
     * @param status the exit status of the mediainfo command (0 for success, non-zero for failure)
     */
    public record Result(Path path, net.mediaarea.mediainfo.MediaInfo mediaInfo, int status) {

        /**
         * Returns the first video track, if any. {@code video().isPresent()} would be a way to check whether the media file is video.
         *
         * @return an {@link Optional} containing the first video track, or empty if no video track is found
         */
        public Optional<TrackType> video() {
            return mediaInfo.getMedias().stream()
                .flatMap(m -> m.getTracks().stream())
                .filter(t -> "Video".equals(t.getTrackType()))
                .findFirst();
        }
        /**
         * Returns the first audio track, if any.
         * @return an {@link Optional} containing the first audio track, or empty if no audio track is found
         */
        public Optional<TrackType> audio() {
            return mediaInfo.getMedias().stream()
                .flatMap(m -> m.getTracks().stream())
                .filter(t -> "Audio".equals(t.getTrackType()))
                .findFirst();
        }

        /**
         * Generic information is in the 'General' track of the media info object. I think this is always present
         *
         * @throws IllegalStateException if no General track is found
         */
        public TrackType general() {
            return mediaInfo.getMedias().stream()
                .flatMap(m -> m.getTracks().stream())
                .filter(t -> "General".equals(t.getTrackType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No General track found in media info for " + path));
        }

        /**
         * Returns the duration of the media file as a {@link Duration} object.
         *
         * @return the duration of the media file
         */
        public Duration duration() {
            return Duration.ofMillis(Math.round(1000L * general().getDuration().doubleValue()));
        }

        /**
         * Returns the overall bit rate of the media file in bits per second.
         *
         * @return the overall bit rate of the media file
         */
        public double bitRate() {
            return general().getOverallBitRate();
        }

        /**
         * @return whether the call to the mediainfo was successful, i.e. the exit code status  was 0.
         */

        public boolean success() {
            return status == 0;
        }

        /**
         * Whether the media file seems to represent a vertical video (i.e., the height of {@link #containingRectangle()}} is greater than the width).
         */
        public boolean vertical() {
            return containingRectangle().map(Rectangle::vertical).orElse(false);
        }


        public String name() {
            return mediaInfo.getMedias().stream().map(MediaType::getRef).filter(StringUtils::isNotBlank).findFirst().orElse("<no name>");
        }

        /**
         * Media can have a 'rotation' (in degrees). For 'portrait' video this is often 90 or 270 degrees.
         *
         */
        public OptionalDouble rotation() {
            return video()
                .map(TrackType::getRotation)
                .stream()
                .mapToDouble(Double::parseDouble)
                .findFirst();
        }

        /**
         * Returns a rectangle that contains the video track, taking into account any rotation.
         * The rectangle's width and height are adjusted based on the rotation of the video track.
         *
         * @return an {@link Optional} containing a {@link Rectangle} that represents the containing rectangle of the video track, or empty if no video track is present
         */
        public Optional<Rectangle> containingRectangle() {
            TrackType trackType = video().orElse(null);

            if (trackType != null) {
                double rotated = trackType.getRotation() == null ? 0 : Math.PI * Double.parseDouble(trackType.getRotation()) / 180.0;

                double sin = Math.sin(rotated);
                double cos = Math.cos(rotated);
                double width = trackType.getWidth().doubleValue();
                double height = trackType.getHeight().doubleValue();
                Rectangle rectangle = new Rectangle(
                    (int) Math.round(Math.abs(width * cos) + Math.abs(height * sin)),
                    (int) Math.round(Math.abs(width * sin) + Math.abs(height * cos))
                );
                return Optional.of(rectangle);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public @NonNull String toString() {
            return (success() ? "" : "FAIL:") + name() + (video().isPresent() ? " (video " + containingRectangle().get().aspectRatio() + ")" :  " (no video track)") + ", bitrate: " + (bitRate() / 1024) + " kbps, duration: " + duration();

        }
    }


}

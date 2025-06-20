package nl.vpro.mediainfo;

import net.mediaarea.mediainfo.MediaType;
import net.mediaarea.mediainfo.TrackType;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.meeuw.math.shapes.dim2.Rectangle;
import org.meeuw.math.uncertainnumbers.field.UncertainReal;

import static org.meeuw.math.uncertainnumbers.field.UncertainRealField.element;

/**
 * A wrapper around {@link net.mediaarea.mediainfo.MediaInfo} (which is the straightforwardly unmarshalled result of the mediainfo command).
 *
 * @param path      the path to the file to get information about
 * @param mediaInfo The unmarshalled result of the call to {@code mediainfo}
 * @param status    the exit status of the mediainfo command (0 for success, non-zero for failure)
 */
public record MediaInfo(Path path, net.mediaarea.mediainfo.MediaInfo mediaInfo, int status) {

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
     *
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
     * Whether the media file seems to represent a vertical video (i.e., the height of {@link #circumscribedRectangle()}} is greater than the width).
     */
    public boolean vertical() {
        return circumscribedRectangle().map(Rectangle::vertical).orElse(false);
    }


    public String name() {
        return mediaInfo.getMedias().stream()
            .map(MediaType::getRef)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .map(n -> new File(n).getName())
            .orElse("<no name>");
    }

    /**
     * Media can have a 'rotation' (in degrees). For a 'portrait' video this is often 90 or 270 degrees.
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
    public Optional<Rectangle<UncertainReal>> circumscribedRectangle() {
        TrackType trackType = video().orElse(null);

        if (trackType != null) {
            double rotated = trackType.getRotation() == null ? 0 : Double.parseDouble(trackType.getRotation());

            return Optional.of(new Rectangle<>(element(
                trackType.getWidth().doubleValue()),
                element(trackType.getHeight().doubleValue()))
                    .rotate(element(Math.toRadians(rotated)))
                .circumscribedRectangle().shape());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public @NonNull String toString() {
        return (success() ? "" : "FAIL:") + (video().isPresent() ? ("video " + circumscribedRectangle().get().aspectRatio()) : " (no video track)") + ", bitrate: " + (bitRate() / 1024) + " kbps, duration: " + duration();

    }
}

package nl.vpro.mediainfo;

import java.time.Duration;

/**
 * {@link MediaInfo} is very big. This can be used to pass only very basic information.
 * @since 5.13
 */
public interface BasicMediaInfo {

    String name();

    Duration duration();

    boolean isVideo();

    boolean vertical();

    String aspectRatio();


}

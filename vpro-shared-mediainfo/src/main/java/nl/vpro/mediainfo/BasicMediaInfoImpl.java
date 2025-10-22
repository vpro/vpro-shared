package nl.vpro.mediainfo;

import java.time.Duration;

public record BasicMediaInfoImpl(String name, Duration duration, boolean isVideo, boolean vertical, String aspectRatio) implements BasicMediaInfo {

    public static BasicMediaInfoImpl audio(String name, Duration duration) {
        return new BasicMediaInfoImpl(name, duration, false, false, null);
    }
}

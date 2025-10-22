package nl.vpro.mediainfo;

import java.time.Duration;

public record BasicMediaInfoImpl(boolean vertical, Duration duration) implements BasicMediaInfo {
}

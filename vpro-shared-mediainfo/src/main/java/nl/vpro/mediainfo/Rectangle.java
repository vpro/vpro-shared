package nl.vpro.mediainfo;

import static org.meeuw.math.IntegerUtils.gcd;

public record Rectangle(int width, int height) {

    boolean vertical() {
        return width < height;
    }

    public String aspectRatio() {
        int ggcd = gcd(height, width);
        return String.format("%d:%d", width / ggcd, height / ggcd);
    }
}

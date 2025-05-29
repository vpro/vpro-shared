package nl.vpro.mediainfo;

import jakarta.validation.constraints.Min;

import static org.meeuw.math.IntegerUtils.gcd;

public record Rectangle(
    @Min(0) int width,
    @Min(0) int height)  {


    boolean vertical() {
        return width < height;
    }

    public String aspectRatio() {
        int gcd = gcd(width, height);
        return String.format("%d:%d", width / gcd, height / gcd);
    }
}

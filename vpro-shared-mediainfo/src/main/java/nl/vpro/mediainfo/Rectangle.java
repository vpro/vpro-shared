package nl.vpro.mediainfo;

import jakarta.validation.constraints.Min;

import static org.meeuw.math.IntegerUtils.gcd;

public record Rectangle(
    @Min(0) int width,
    @Min(0) int height)  {

    /**
     * Checks if the rectangle is vertical, meaning its width is smaller than its height.
     *
     * @return true if the rectangle is vertical, false otherwise
     */
    boolean vertical() {
        return width < height;
    }

    /**
     * Rotates the rectangle by a given angle in radians.
     * The new width and height are calculated based on the rotation transformation.
     *
     * @param angle the angle in radians to rotate the rectangle
     * @return a new Rectangle object with the rotated dimensions
     */
    Rectangle rotate(double angle) {

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new Rectangle(
            (int) Math.round(Math.abs(width * cos) + Math.abs(height * sin)),
            (int) Math.round(Math.abs(width * sin) + Math.abs(height * cos))
        );
    }

    /**
     * A convenience method to {@link #rotate(double)} the rectangle by a given angle in degrees (using {@link Math#toRadians(double)}).
     */
    Rectangle rotateDegrees(Double angle) {
        if (angle == null) {
            return this;
        }
        return rotate(Math.toRadians(angle));
    }


    /**
     * Returns the aspect ratio of the rectangle in the format "width:height".
     * The values are reduced to their simplest form using the greatest common divisor (GCD).
     *
     * @return a string representing the aspect ratio
     */
    public String aspectRatio() {
        int gcd = gcd(width, height);
        return String.format("%d:%d", width / gcd, height / gcd);
    }
}

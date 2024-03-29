package nl.vpro.i18n;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

/**
 * Some utility for working in the Netherlands
 *
 * @author Michiel Meeuwissen
 * @since 2.8
 */
public class Dutch {

    private  Dutch() {

    }

    /**
     * The {@link ZoneId} in the Netherlands
     */
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Amsterdam");



    private static final DateTimeFormatter FORMATTER_VERY_LONG;
    private static final DateTimeFormatter FORMATTER_LONG;
    private static final DateTimeFormatter FORMATTER_SHORT;
    static {
        FORMATTER_VERY_LONG =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locales.DUTCH)
                .withZone(ZONE_ID);
        FORMATTER_LONG =
            DateTimeFormatter.ofPattern("d MMMM HH:mm", Locales.DUTCH)
                .withZone(ZONE_ID);
        FORMATTER_SHORT =
            DateTimeFormatter.ofPattern("HH:mm", Locales.DUTCH)
                .withZone(ZONE_ID);
    }

    /**
     * Like {@link #formatSmartly(Temporal)}, but with the option to specify the {@link Clock}, useful for testing.
     */
    public static String formatSmartly(Clock clock, Temporal instant)
    {
        return formatSmartly(clock.instant(), instant);
    }

    /**
     * Like {@link #formatSmartly(Temporal)}, but with the option to specify 'now'. Mainly useful for testing.
     */
    public static String formatSmartly(Temporal now, Temporal instant) {

        Duration distance = Duration.between(now, instant).abs();
        if (distance.compareTo(Duration.ofHours(12)) < 0) {
            return FORMATTER_SHORT.format(instant);
        } else if (distance.compareTo(Duration.ofDays(365).dividedBy(2)) < 0) {
            return FORMATTER_LONG.format(instant);
        } else {
            return FORMATTER_VERY_LONG.format(instant);
        }
    }

    /**
     * Formats a date time smartly, I.e. it will use shorter representation if this temporal is closer to {@link Instant#now()}.
     *<ul>
     * <li>If it's closer than 12 hour, then only a time will be displayed,</li>
     * <li>If it's closer than half a year, then a date and a time will be displayed (but without an year)</li>
     * <li>Otherwise a full date time string will be returned.</li>
     * </ul>
     */
    public static String formatSmartly(Temporal instant) {
        return formatSmartly(Instant.now(), instant);
    }

}

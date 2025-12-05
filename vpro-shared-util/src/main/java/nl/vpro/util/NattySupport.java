package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;

import org.natty.DateGroup;
import org.natty.Parser;



/**
 * The dependency on natty in {@link StringInstantToJsonTimestamp} is optional. Put support for it in this class, so we can just catch the resulting NoClassDefFoundError.
 * @author Michiel Meeuwissen
 * @since 2.14
 */

@Slf4j
public class NattySupport {
    private static final Parser PARSER = new Parser(TimeZone.getTimeZone(BindingUtils.DEFAULT_ZONE));


    public static Optional<Instant> parseDate(String value) {
        List<DateGroup> groups = PARSER.parse(value);
        if (groups.size() == 1) {
            log.info("Parsed date '{}' to {}", value, groups.get(0).getDates());
            return Optional.ofNullable(DateUtils.toInstant(groups.get(0).getDates().get(0)));
        }
        return Optional.empty();
    }
}

package nl.vpro.jackson2;

import java.time.Instant;
import java.util.*;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import nl.vpro.util.BindingUtils;
import nl.vpro.util.DateUtils;

/**
 * The dependency on natty in {@link StringInstantToJsonTimestamp} is optional. Put support for it in this class so we can just catch the resulting NoClassDefFoundError.
 * @author Michiel Meeuwissen
 * @since 2.14
 */

class NattySupport {
    private static final Parser PARSER = new Parser(TimeZone.getTimeZone(BindingUtils.DEFAULT_ZONE));


    static Optional<Instant> parseDate(String value) {
        List<DateGroup> groups = PARSER.parse(value);
        if (groups.size() == 1) {
            return Optional.ofNullable(DateUtils.toInstant(groups.get(0).getDates().get(0)));
        }
        return Optional.empty();
    }
}

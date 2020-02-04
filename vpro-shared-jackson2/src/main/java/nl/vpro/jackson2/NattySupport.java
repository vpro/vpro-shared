package nl.vpro.jackson2;

import java.time.Instant;
import java.util.*;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import nl.vpro.util.BindingUtils;
import nl.vpro.util.DateUtils;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */

class NattySupport {
    private static Parser PARSER = new Parser(TimeZone.getTimeZone(BindingUtils.DEFAULT_ZONE));


    static Optional<Instant> parseDate(String value) {
        List<DateGroup> groups = PARSER.parse(value);
        if (groups.size() == 1) {
            return Optional.of(DateUtils.toInstant(groups.get(0).getDates().get(0)));
        }
        return Optional.empty();
    }
}

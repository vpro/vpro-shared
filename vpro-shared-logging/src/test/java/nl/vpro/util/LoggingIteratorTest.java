package nl.vpro.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Iterator;

import nl.vpro.logging.LoggingIterator;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
class LoggingIteratorTest {


    @Test
    public void test() {
        Iterator<String> i = Arrays.asList("a", "b", "c", "d").iterator();

        LoggingIterator<String> logging = LoggingIterator
            .<String>builder()
            .wrapped(i)
            .level(Level.INFO)
            .logger(log)
            .interval(2)
            .build();

        logging.forEachRemaining((s) -> {
            log.info("{}", s);
        });

    }

}

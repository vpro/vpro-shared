package nl.vpro.logging.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.event.Level;

/**
 * @author Michiel Meeuwissen
 * @since 1.76
 */
public class ChainedSimpleLogger implements  SimpleLogger {

    final List<SimpleLogger> list = new ArrayList<>();


    public ChainedSimpleLogger(SimpleLogger... wrapped) {
        Collections.addAll(list, wrapped);
    }



    @Override
    public void accept(Level level, String message, Throwable t) {
        for (SimpleLogger logger : list) {
            logger.accept(level, message, t);;
        }
    }
}

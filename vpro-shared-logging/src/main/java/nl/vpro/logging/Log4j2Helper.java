package nl.vpro.logging;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class Log4j2Helper {


    public static void debugOrInfo(Logger logger, boolean info, String format, Object... argArray) {
        logger.log(info ? Level.INFO : Level.DEBUG, format, argArray);
    }

}

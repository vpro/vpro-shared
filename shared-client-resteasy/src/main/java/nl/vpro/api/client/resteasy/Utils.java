package nl.vpro.api.client.resteasy;

import java.util.Optional;
import java.util.function.Supplier;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */
public class Utils {
    private static Logger LOG = LoggerFactory.getLogger(Utils.class);


    public static <T> Optional<T> wrapNotFound(Supplier<T> t) {

        try {
            return Optional.ofNullable(t.get());
        } catch (NotFoundException nfe) {
            LOG.debug(nfe.getMessage());
            return Optional.empty();
        }
    }
}

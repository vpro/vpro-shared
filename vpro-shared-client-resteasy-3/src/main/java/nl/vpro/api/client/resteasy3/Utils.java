package nl.vpro.api.client.resteasy3;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.NotFoundException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Michiel Meeuwissen
 * @since 0.47
 */

@Slf4j
public class Utils {


    public static <T> Optional<T> wrapNotFound(Supplier<T> t) {

        try {
            return Optional.ofNullable(t.get());
        } catch (NotFoundException nfe) {
            log.debug(nfe.getMessage());
            return Optional.empty();
        }
    }
}

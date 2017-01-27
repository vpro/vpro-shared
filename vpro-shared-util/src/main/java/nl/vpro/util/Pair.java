package nl.vpro.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Data
@AllArgsConstructor(staticName = "of")
public class Pair<F, S> {
    private F first;
    private S second;
}


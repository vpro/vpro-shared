package nl.vpro.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Data
@AllArgsConstructor(staticName = "of")
@Builder
public class Pair<F, S> {
    private F first;
    private String firstDescription = "first";

    private S second;
    private String secondDescription = "second";

    @Override
    public String toString() {
        return "(" + firstDescription + "=" + first + ", " + secondDescription + "=" + second + ")";
    }
}


package nl.vpro.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Data
@AllArgsConstructor(staticName = "of")
@lombok.Builder(builderClassName = "Builder")
public class Pair<F, S> {
    private F first;
    @lombok.Builder.Default
    private String firstDescription = "first";

    private S second;
    @lombok.Builder.Default
    private String secondDescription = "second";

    public static <F, S> Pair<F, S> of(F first, S second){
        return Pair.<F, S>builder().first(first).second(second).build();

    }

    @Override
    public String toString() {
        return "(" + firstDescription + "=" + first + ", " + secondDescription + "=" + second + ")";
    }
}


package nl.vpro.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * @author Michiel Meeuwissen
 * @since 1.63
 */
@Data
@AllArgsConstructor(staticName = "of")
@lombok.Builder(builderClassName = "Builder")

public class Pair<F, S> implements Map.Entry<F, S> {

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

    @Override
    public F getKey() {
        return getFirst();
    }

    @Override
    public S getValue() {
        return getSecond();
    }

    @Override
    public S setValue(S value) {
        S prev = second;
        setSecond(value);
        return prev;
    }

    public static class Builder<F, S> {

        public Builder<F, S> key(F value) {
            return descriptedFirst("key", value);
        }

        public Builder<F, S> value(S value) {
            return descriptedSecond("value", value);
        }


        public Builder<F, S> descriptedFirst(String description, F value) {
            return
                firstDescription(description).first(value);
        }
        public Builder<F, S> descriptedSecond(String description, S value) {
            return
                secondDescription(description).second(value);
        }
    }
}


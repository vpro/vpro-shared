package nl.vpro.util;

import lombok.Getter;

import java.util.*;
import java.util.function.BooleanSupplier;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Michiel Meeuwissen
 * @since 0.42
 */
public enum Env {
    LOCALHOST,

    TEST,
    TESTA(TEST),
    TESTB(TEST),
    TEST_NEW,
    TEST_OLD,
    TESTA_NEW(TEST_NEW),
    TESTB_NEW(TEST_NEW),

    ACC,
    ACCA(ACC),
    ACCB(ACC),
    ACC_NEW,
    ACC_OLD,
    ACCA_NEW(ACC_NEW),
    ACCB_NEW(ACC_NEW),

    PROD,
    PRODA(PROD),
    PRODB(PROD),
    PROD_NEW,
    PROD_OLD,
    PRODA_NEW(PROD_NEW),
    PRODB_NEW(PROD_NEW);

    @Getter
    private final List<Env> fallbacks;

    Env(Env... fallbacks) {
        this.fallbacks = fallbacksList(fallbacks);
    }

    private static List<Env> fallbacksList(Env... fallbacks) {
        List<Env> result = new ArrayList<>();
        result.add(null);
        result.addAll(Arrays.asList(fallbacks));
        return Collections.unmodifiableList(result);
    }

    public static Optional<Env> optionalValueOf(@Nullable String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            Env foundEnv = Env.valueOf(value.toUpperCase());
            return Optional.of(foundEnv);
        } catch (IllegalArgumentException iae) {
            return Optional.empty();
        }
    }
    /**
     * @return 1 if env if exact match, 0 if fallback match, -
     */
    public Match matches(Env env) {
        if (env == this) {
            return Match.EXACT;
        }
        if (fallbacks.contains(env)) {
            return Match.ON_FALLBACK;
        }
        return Match.NONE;
    }

    public enum Match implements BooleanSupplier {
        EXACT(true, 1),
        ON_FALLBACK(true, 0),
        NONE(false, -1);

        final boolean match;
        @Getter
        final int strength;

        Match(boolean match, int strength) {
            this.match = match;
            this.strength = strength;
        }

        @Override
        public boolean getAsBoolean() {
            return match;
        }
    }
}

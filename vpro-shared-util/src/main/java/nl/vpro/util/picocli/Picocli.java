package nl.vpro.util.picocli;

import picocli.CommandLine;

import java.time.Duration;
import java.time.Instant;

import nl.vpro.util.*;

/**
 * Provides some convertors for the <a href="https://picocli.info/">picocli</a> command line parser, wich wrap some tools which are in this module.
 * <p>
 * The dependency on picocli is optional, so these classes are only usable if you have picocli anyway.
 *
 * @author Michiel Meeuwissen
 * @since 2.12
 */
public class Picocli {
    private Picocli() {

    }

    public static class EnvConverter implements CommandLine.ITypeConverter<Env> {
        @Override
        public Env convert(String value) {
            return Env.valueOf(value.toUpperCase());
        }
    }

    public static class InstantConverter implements CommandLine.ITypeConverter<Instant> {
        @Override
        public Instant convert(String value) {
            return TimeUtils.parse(value).orElse(Instant.now());
        }
    }

    public static class DurationConverter implements CommandLine.ITypeConverter<Duration> {
        @Override
        public Duration convert(String value) {
            return TimeUtils.parseDuration(value).orElseThrow(() -> new IllegalArgumentException(value));
        }
    }

    public static class IntegerVersionConverter implements CommandLine.ITypeConverter<IntegerVersion> {
        @Override
        public IntegerVersion convert(String value) {
            return IntegerVersion.parseIntegers(value);
        }
    }
}

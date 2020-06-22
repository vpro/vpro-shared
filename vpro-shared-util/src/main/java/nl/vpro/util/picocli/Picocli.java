package nl.vpro.util.picocli;

import picocli.CommandLine;

import java.time.Instant;

import nl.vpro.util.Env;
import nl.vpro.util.TimeUtils;

/**
 * Provides some convertors for picocli command line parser, wich wrap some tools which are in this module.
 *
 * The depenency on picocli is optional, so these classes are only useable if you havva picocli anyway.
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
    public static class InstantConvert implements CommandLine.ITypeConverter<Instant> {
        @Override
        public Instant convert(String value) {
            return TimeUtils.parse(value).orElse(Instant.now());
        }
    }
}

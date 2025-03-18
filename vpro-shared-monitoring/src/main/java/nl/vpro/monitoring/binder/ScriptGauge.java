package nl.vpro.monitoring.binder;

import io.micrometer.core.instrument.Tag;

import java.util.List;
import java.util.stream.Stream;

public record ScriptGauge(
    String name,
    Iterable<Tag> tags,
    Double value
) {

    public static ScriptGauge parse(String l) {
        String[] split = l.split("\t");
        String name = split[0];
        Double d = Double.parseDouble(split[1]);
        List<Tag> tags = Stream.of(split[2].split(",")).map(s -> {
            var spl = s.split("=", 2);
            return Tag.of(spl[0], spl[1]);
        }).toList();
        return new ScriptGauge(name, tags, d);

    }

    public String key() {
        return name() + " " + tags;
    }


}

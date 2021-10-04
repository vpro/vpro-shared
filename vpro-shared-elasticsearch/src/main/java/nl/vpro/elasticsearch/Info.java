package nl.vpro.elasticsearch;

import lombok.*;

import nl.vpro.util.IntegerVersion;

@AllArgsConstructor
@lombok.Builder
@Data
public class Info {

    private final String name;
    private final String clusterName;
    private final Distribution distribution;
    private final String buildFlavor;
    private final IntegerVersion version;
    private final IntegerVersion luceneVersion;

}

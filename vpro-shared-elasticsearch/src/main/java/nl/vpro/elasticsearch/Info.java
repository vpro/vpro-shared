package nl.vpro.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@lombok.Builder
@Getter
public class Info {

    private final String name;
    private final String clusterName;
    private final Distribution distribution;

}

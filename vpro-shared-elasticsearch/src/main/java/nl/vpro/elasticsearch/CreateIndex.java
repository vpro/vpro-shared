package nl.vpro.elasticsearch;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@lombok.Data

public class CreateIndex {

    private final boolean useNumberPostfix;

    private final boolean forReindex;

    private final Runnable  callBack;

    private final boolean createAliases;

    private final Integer shards;

    private final Integer numberOfReplicas;

    private final boolean requireMappings;

    public static final CreateIndex DEFAULT = CreateIndex.builder().build();

    public static final CreateIndex FOR_TEST = CreateIndex.builder()
        .createAliases(false)
        .useNumberPostfix(false)
        .numberOfReplicas(0)
        .shards(1)
        .build();


    @lombok.Builder(builderClassName = "Builder")
    private  CreateIndex(
        boolean useNumberPostfix,
        boolean forReindex,
        Runnable callBack,
        Boolean createAliases,
        Integer shards,
        Integer numberOfReplicas,
        Boolean requireMappings) {
        this.useNumberPostfix = useNumberPostfix;
        this.forReindex = forReindex;
        this.callBack = callBack;
        this.createAliases = createAliases == null || createAliases;
        this.shards = shards;
        this.numberOfReplicas = numberOfReplicas;
        this.requireMappings = requireMappings == null || requireMappings;
    }
}

package nl.vpro.elasticsearch;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Options for creating indices
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@lombok.Data
public class CreateIndex {

    /**
     * Postfix the index name with the first free number
     */
    private final boolean useNumberPostfix;

    /**
     * Configure the index for 'reindex', this is normally desired for new indices which are to be filled with existing data
     * Update times are infinite, no replicas
     */
    private final boolean forReindex;

    /**
     * Something to run when finished
     */
    private final Runnable  callBack;

    private final boolean createAliases;

    private final Integer shards;

    private final Integer numberOfReplicas;

    private final boolean requireMappings;

    public static final CreateIndex DEFAULT = CreateIndex.builder()
        .createAliases(true)
        .useNumberPostfix(true)
        .build();

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
        @Nullable Runnable callBack,
        @Nullable Boolean createAliases,
        @Nullable Integer shards,
        @Nullable Integer numberOfReplicas,
        @Nullable Boolean requireMappings) {
        this.useNumberPostfix = useNumberPostfix;
        this.forReindex = forReindex;
        this.callBack = callBack;
        this.createAliases = createAliases == null || createAliases;
        this.shards = shards;
        this.numberOfReplicas = numberOfReplicas;
        this.requireMappings = requireMappings == null || requireMappings;
    }
}

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

    public static final CreateIndex DEFAULT = CreateIndex.builder().build();

    @lombok.Builder(builderClassName = "Builder")
    public CreateIndex(boolean useNumberPostfix, boolean forReindex, Runnable callBack, Boolean createAliases, Integer shards) {
        this.useNumberPostfix = useNumberPostfix;
        this.forReindex = forReindex;
        this.callBack = callBack;
        this.createAliases = createAliases == null || createAliases;
        this.shards = shards;
    }
}

package nl.vpro.elasticsearch;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
@lombok.Data
@lombok.AllArgsConstructor
@lombok.Builder(builderClassName = "Builder")
public class CreateIndex {

    private boolean useNumberPostfix;

    private boolean forReindex;

    private Runnable  callBack;

    private boolean createAliases;

    public static final CreateIndex DEFAULT = CreateIndex.builder().build();

}

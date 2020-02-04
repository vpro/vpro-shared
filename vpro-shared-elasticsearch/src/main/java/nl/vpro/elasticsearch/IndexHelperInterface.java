package nl.vpro.elasticsearch;

/**
 * @author Michiel Meeuwissen
 * @since 2.9
 */
public interface IndexHelperInterface<C> {

    void createIndex(CreateIndex createIndex);

    default void createIndex() {
        createIndex(CreateIndex.DEFAULT);
    }
    /**
     * Checks whether index exists
     */
    boolean checkIndex();

    long count();

    /**
     * Checks whether index exists, and if not, created it.
     */
    default boolean  createIndexIfNotExists(CreateIndex createIndex) {
        if (! checkIndex()) {
            createIndex(createIndex);
            return true;
        } else {
            return false;
        }
    }

    default boolean  createIndexIfNotExists() {
        return createIndexIfNotExists(CreateIndex.DEFAULT);
    }

    C client();


}

package nl.vpro.hibernate.search6;

import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.builtin.impl.DefaultBooleanBridge;


import nl.vpro.hibernate.search6.domain.MyEnum;
import nl.vpro.hibernate.search6.domain.TestEntity;

public class MyEntityMapper implements HibernateOrmSearchMappingConfigurer , LuceneAnalysisConfigurer
{

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {

        var mapping = context.programmaticMapping();
        var testEntity = mapping.type(TestEntity.class);

        testEntity.indexed();
        testEntity.property("text")
            .fullTextField();
            //.analyzer("dutch");

        testEntity.property("myEnum")
            .keywordField()
            .valueBridge(new EnumToLowerCaseBridge<>(MyEnum.class) {})
            .projectable(Projectable.YES)
        ;
        testEntity.property("instant")
            .genericField()
            .projectable(Projectable.YES)
            .searchable(Searchable.YES)
            ;


        testEntity.property("subObject")
            .binder(new TestBridge())
            ;


        testEntity.property("list")
            .binder(new CollectionSizeBridge.Binder<String>());

        testEntity.property("list")
            .binder(new CollectionSizeBridge.Binder<String>()
                .withName("filteredSize")
                .withCollectionFilter(c -> c.startsWith("a")));

        testEntity.property("myBoolean")
            .genericField()
            .valueBridge(DefaultBooleanBridge.INSTANCE)
            .projectable(Projectable.YES);

    }

    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer( "default" ).instance(new DutchAnalyzer());

    }
}

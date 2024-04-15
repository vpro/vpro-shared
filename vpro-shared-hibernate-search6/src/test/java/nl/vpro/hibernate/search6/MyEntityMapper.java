package nl.vpro.hibernate.search6;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;


import nl.vpro.hibernate.search6.domain.TestEntity;

public class MyEntityMapper implements org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer , LuceneAnalysisConfigurer
{

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {

        var mapping = context.programmaticMapping();
        var test = mapping.type(TestEntity.class);

        test.indexed();

        test.property("text")
            .fullTextField();
            //.analyzer("dutch");

        test.property("myEnum")
            .keywordField()
            //.valueBinder(DefaultEnumBridge.Binder.INSTANCE)
            //.valueBridge( new DefaultEnumBridge<MyEnum>(MyEnum.class))
            //.valueBridge(new EnumToLowerCaseBridge(MyEnum.class))
        ;
        test.property("instant")
            .genericField()
            .valueBridge(new InstantToEpochMillisBridge());

        test.property("instant")
            .genericField("instant2");

    }

    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer( "dutch" ).custom()
                .tokenizer( "standard" );

    }
}

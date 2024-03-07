package nl.vpro.configuration.spring.converters;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;

import java.util.Set;


import com.google.common.annotations.Beta;

@Configuration
@Beta
public class CustomConversion {


    @Bean
    public ConversionServiceFactoryBean customService() {
        ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();

        factory.setConverters(Set.of(
            StringToDurationConverter.class,
            StringToTemporalAmountConverter.class,
            StringToIntegerListConverter.class,
            StringToLocalTimeConverter.class,
            StringToInstantConverter.class,
            StringToLocalDateTimeConverter.class
        ));
        return factory;
    }
}

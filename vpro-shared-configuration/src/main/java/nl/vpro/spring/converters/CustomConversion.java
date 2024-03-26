package nl.vpro.spring.converters;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.google.common.annotations.Beta;

@Configuration
@Beta
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomConversion {


    @Bean
    public ConversionServiceFactoryBean conversionService() {
        ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();

        factory.setConverters(Set.of(
            new StringToDurationConverter(),
            new StringToTemporalAmountConverter(),
            new StringToIntegerListConverter(),
            new StringToLocalTimeConverter(),
            new StringToInstantConverter(),
            new StringToLocalDateTimeConverter()
        ));
        return factory;
    }
}

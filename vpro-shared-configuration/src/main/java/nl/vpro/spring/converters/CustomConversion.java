package nl.vpro.spring.converters;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = true)
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomConversion {



    @Bean
    public ConversionServiceFactoryBean conversionService() {
        ConversionServiceFactoryBean factory = new ConversionServiceFactoryBean();

        Set<?> set = Set.of(
            new StringToDurationConverter(),
            new StringToTemporalAmountConverter(),
            new StringToIntegerListConverter(),
            new StringToLocalTimeConverter(),
            new StringToInstantConverter(),
            new StringToLocalDateTimeConverter()
        );
        factory.setConverters(set);
        log.info("Installed custom conversion {}" ,set);
        return factory;
    }
}

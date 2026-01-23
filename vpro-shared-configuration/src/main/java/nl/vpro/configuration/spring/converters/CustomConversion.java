package nl.vpro.configuration.spring.converters;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Registers all converters in {@link nl.vpro.configuration.spring.converters}.
 */
@Configuration(proxyBeanMethods = true)
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomConversion {

    @Bean
    public ConversionServiceFactoryBean conversionService() {
        var factory = new ConversionServiceFactoryBean();
        var set = Set.of(
            new StringToDurationConverter(),
            new StringToTemporalAmountConverter(),
            new StringToIntegerListConverter(),
            new StringToLocalTimeConverter(),
            new StringToInstantConverter(),
            new StringToLocalDateTimeConverter(),
            new StringToLocalDateConverter(),
            new StringToIntegerConverter(),
            new StringToBigIntegerConverter(),
            new StringToLongConverter()
        );
        factory.setConverters(set);
        log.info("Installed custom conversion {}" ,set);
        return factory;
    }
}

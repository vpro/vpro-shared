package nl.vpro.monitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@EnableWebSecurity
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MonitoringWebSecurityConfiguration {


    /**
     * Just permits on the /manage endpoints and /.well-known paths. Metrics call is security by itself.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain monitoringWebSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatchers(config -> config.requestMatchers(
            antMatcher("/manage/**"),
            antMatcher("/.well-known/**")
            )
        );
        http.authorizeHttpRequests(config ->
            config.anyRequest().permitAll());

        return http.build();
    }



}

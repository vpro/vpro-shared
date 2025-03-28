package nl.vpro.monitoring.config;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@EnableWebSecurity
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MonitoringWebSecurityConfiguration {

    private final MonitoringProperties properties;

    @Inject
    public MonitoringWebSecurityConfiguration(MonitoringProperties properties, AuthenticationManagerBuilder auth) throws Exception {
        this.properties = properties;
        configure(auth);
    }


    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain monitoringWebSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatchers(config -> config.requestMatchers(
            antMatcher("/manage/**"),
            antMatcher("/.well-known/**")
            )
        );


        http.authorizeHttpRequests((authz) -> {
            if (properties.isHealthPermitAll()) {

                authz.requestMatchers(
                    antMatcher("/manage/health")
                ).permitAll();
            }
            authz.requestMatchers(
                antMatcher("/.well-known/**")
            ).permitAll();
            authz.
                requestMatchers(
                    antMatcher("/manage/**"))
                .hasRole("MANAGER");
        }).httpBasic(
            httpBasic -> httpBasic.realmName("manager"));
        return http.build();
    }


    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        auth.inMemoryAuthentication()
            .passwordEncoder(encoder)
            .withUser(properties.getUser())
            .password(encoder.encode(properties.getPassword()))
            .roles("MANAGER")
        ;
    }


}

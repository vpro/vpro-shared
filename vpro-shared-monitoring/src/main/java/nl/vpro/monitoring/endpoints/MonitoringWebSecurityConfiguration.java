package nl.vpro.monitoring.endpoints;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import nl.vpro.monitoring.config.MonitoringProperties;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@Order(-1)
public class MonitoringWebSecurityConfiguration { // deprecated damn.

    private final MonitoringProperties properties;

    @Inject
    public MonitoringWebSecurityConfiguration(MonitoringProperties properties, AuthenticationManagerBuilder auth) throws Exception {
        this.properties = properties;
        configure(auth);
    }

    @Bean
    public SecurityFilterChain monitoringSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> {
                if (properties.isHealthPermitAll()) {
                    authz.requestMatchers(antMatcher("/manage/health")).permitAll();
                }
                try {
                    authz.requestMatchers(antMatcher("/manage/**"))
                        .hasRole("MANAGER")
                        .and()
                        .httpBasic()
                        .realmName("management")
                    ;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        return http.build();
    }


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(antMatcher("/resources/**"));
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

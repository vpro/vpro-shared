package nl.vpro.monitoring.endpoints;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import nl.vpro.monitoring.config.MonitoringProperties;

@Configuration
@EnableWebSecurity
@Order(-1)
public class MonitoringWebSecurityConfiguration  { // deprecated damn.

    private final MonitoringProperties properties;

    public MonitoringWebSecurityConfiguration(MonitoringProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry expressionInterceptUrlRegistry = http.
        .antMatcher("/manage/**")
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER)
            .and()
            .authorizeRequests();

        if (properties.isHealthPermitAll()) {
            expressionInterceptUrlRegistry.antMatchers("/manage/health").permitAll();
        }

        expressionInterceptUrlRegistry.antMatchers("/manage/**").hasRole("MANAGER")
            .and()
            .httpBasic()
            .   realmName("management")
        ;
        http
            .authorizeHttpRequests((authz) -> authz
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults());
        return http.build();

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        auth.inMemoryAuthentication()
            .passwordEncoder(encoder)
            .withUser(properties.getUser())
            .password(encoder.encode(properties.getPassword()))
            .roles("MANAGER")
        ;
    }
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().antMatchers("/ignore1", "/ignore2");
    }

    //@Override
    protected void configure(HttpSecurity http) throws Exception {

    }
}

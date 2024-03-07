package nl.vpro.monitoring.endpoints;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import nl.vpro.monitoring.config.MonitoringProperties;


/**
 * Version that still uses the deprecated {@link WebSecurityConfigurerAdapter}.
 * @deprecated use {@link MonitoringWebSecurityConfiguration}
 */

@Configuration
@EnableWebSecurity
@Order(-1)
public class DeprecatedMonitoringWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter { // deprecated damn.

    private final MonitoringProperties properties;

    public DeprecatedMonitoringWebSecurityConfigurerAdapter(MonitoringProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        auth.inMemoryAuthentication()
            .passwordEncoder(encoder)
            .withUser(properties.getUser())
            .password(encoder.encode(properties.getPassword()))
            .roles("MANAGER")
        ;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry expressionInterceptUrlRegistry = http.antMatcher("/manage/**")
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
    }
}

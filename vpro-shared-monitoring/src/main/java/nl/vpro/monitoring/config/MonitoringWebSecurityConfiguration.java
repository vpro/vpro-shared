package nl.vpro.monitoring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
@Order(1)
public class MonitoringWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final MonitoringProperties properties;

    public MonitoringWebSecurityConfiguration(MonitoringProperties properties) {
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
        http.antMatcher("/manage/**")
            .sessionManagement()
            .   sessionCreationPolicy(SessionCreationPolicy.NEVER)
            .and()
            .authorizeRequests()
            .   antMatchers("/manage/health").permitAll()
            .   antMatchers("/manage/**").hasRole("MANAGER")
            .and()
            .httpBasic()
            .   realmName("management")
        ;
    }
}

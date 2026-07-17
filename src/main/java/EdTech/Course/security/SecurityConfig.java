package EdTech.Course.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests(auth -> auth
                .antMatchers(HttpMethod.GET, "/courses/**").permitAll() // Read access is public
                .antMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // Observability and docs
                .anyRequest().authenticated() // All other modifications (enrollment, rating, etc) require auth
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .addFilterAfter(new SecurityAuditFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

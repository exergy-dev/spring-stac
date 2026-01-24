package org.stac.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the STAC API server.
 * <p>
 * Configures endpoint access rules:
 * - Public read access to STAC endpoints (GET requests)
 * - Public search access (POST /search)
 * - Authenticated access required for write operations (POST, PUT, DELETE to /collections/**)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * Security filter chain when security is disabled.
     * Permits all requests without authentication.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @ConditionalOnProperty(name = "stac.security.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain securityFilterChainDisabled(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * Security filter chain when security is enabled.
     * Enforces authentication for write operations.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @ConditionalOnProperty(name = "stac.security.enabled", havingValue = "true")
    public SecurityFilterChain securityFilterChainEnabled(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public read access to STAC endpoints
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/conformance").permitAll()
                        .requestMatchers(HttpMethod.GET, "/collections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/search").permitAll()
                        // Public POST to /search
                        .requestMatchers(HttpMethod.POST, "/search").permitAll()
                        // Actuator endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        // API documentation
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Write operations require authentication
                        .requestMatchers(HttpMethod.POST, "/collections/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/collections/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/collections/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }
}

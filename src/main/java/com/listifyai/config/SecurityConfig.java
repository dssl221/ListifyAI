package com.listifyai.config;

import com.listifyai.security.SupabaseJwtFilter;
import com.listifyai.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@SuppressWarnings({"null", "unchecked", "unused"})
public class SecurityConfig {

    private final SupabaseJwtFilter supabaseJwtFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(SupabaseJwtFilter supabaseJwtFilter,
                          RateLimitFilter rateLimitFilter) {
        this.supabaseJwtFilter = supabaseJwtFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — REST API with stateless JWT auth
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless — no HTTP session
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints ──────────────────────────────────────
                // Supabase handles auth on the frontend; /api/auth/** is a
                // pass-through namespace in case we add auth-adjacent endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()

                // Stripe webhook must be reachable without a Bearer token
                .requestMatchers(HttpMethod.POST, "/api/billing/webhook").permitAll()

                // Actuator endpoints (health, prometheus)
                .requestMatchers(HttpMethod.GET,  "/actuator/**").permitAll()

                // Public homepage data
                .requestMatchers("/api/public/**").permitAll()

                // ── All other API routes require a valid Supabase JWT ─────
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/api/**").authenticated()

                // Non-API paths (e.g. future static resources) are open
                .anyRequest().permitAll()
            )

            // Insert our JWT validation filter before Spring's default auth filter
            .addFilterBefore(supabaseJwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, SupabaseJwtFilter.class);

        return http.build();
    }
}

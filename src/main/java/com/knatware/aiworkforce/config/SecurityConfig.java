package com.knatware.aiworkforce.config;

import com.knatware.aiworkforce.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security with per-role authorization.
 *
 * Public:    the welcome page, the SPA, login, and the H2 console.
 * ADMIN:     full access including staff/company configuration writes.
 * OPERATOR:  read + chat (can run conversations) but not company/staff config.
 *
 * Tokens are obtained from POST /api/auth/login and sent as
 * "Authorization: Bearer <token>".
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(h -> h.frameOptions(f -> f.disable())) // allow H2 console frames
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // allow internal forwards (e.g. /app/ -> /app/index.html) and error dispatches
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD,
                                        jakarta.servlet.DispatcherType.ERROR).permitAll()
                // --- public ---
                .requestMatchers("/", "/index.html", "/app/**", "/static/**",
                                 "/favicon.ico", "/error", "/ws/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/portal/settings").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // --- admin-only writes ---
                // staff & company config: any authenticated user (single-admin friendly)
                .requestMatchers("/api/staff/**").authenticated()
                .requestMatchers("/api/company/**").authenticated()
                // --- meetings & HR: any authenticated user ---
                .requestMatchers("/api/meetings/**").authenticated()
                .requestMatchers("/api/hr/**").authenticated()
                .requestMatchers("/api/emails/**").authenticated()
                .requestMatchers("/api/audit/**").authenticated()
                .requestMatchers("/api/staff-import/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/portal/settings").authenticated()
                // --- read + chat: any authenticated user ---
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

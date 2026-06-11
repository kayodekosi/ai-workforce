package com.knatware.aiworkforce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security setup. HTTP Basic for simplicity (swap for JWT/OAuth in production —
 * noted in the roadmap). The seeded default admin is forced to change its
 * password at first login (enforced in the auth/account controller).
 *
 * NOTE: for an easy first run and API exploration, most endpoints are open in
 * this starter configuration; tightening per-role access is a documented
 * production step. AI staff never authenticate — only human operators do.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // starter: open for exploration; lock down in prod
            )
            .httpBasic(basic -> {});
        return http.build();
    }
}

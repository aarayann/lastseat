package com.example.bookingengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // JSON API with no browser forms, so no CSRF tokens
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/ping", "/actuator/health", "/error",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**", "/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/approvals/**").hasRole("APPROVER")
                        .requestMatchers("/api/bookings/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Demo users. Employee usernames match the employees created by DataSeeder. */
    @Bean
    public UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build(),
                User.withUsername("approver").password(encoder.encode("approver123")).roles("APPROVER").build(),
                employee("aarav", encoder),
                employee("priya", encoder),
                employee("meera", encoder),
                employee("rohan", encoder),
                employee("kavya", encoder),
                employee("ishaan", encoder));
    }

    private UserDetails employee(String username, PasswordEncoder encoder) {
        return User.withUsername(username).password(encoder.encode("password")).roles("EMPLOYEE").build();
    }
}




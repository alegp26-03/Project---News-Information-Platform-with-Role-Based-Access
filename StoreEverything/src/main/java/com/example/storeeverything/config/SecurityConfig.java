package com.example.storeeverything.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * This file is the security "rulebook" for our web app.
 * It decides who can go where, and handles logging in and out.
 */
@Configuration // Tells Spring: "This is a setup file."
@EnableWebSecurity // Turns on web security features.
public class SecurityConfig {

    /**
     * Tool to scramble (encode) passwords for safety in the database.
     *
     * @return The password scrambling tool.
     */
    @Bean // Spring creates and manages this tool.
    public PasswordEncoder passwordEncoder() {
        // Uses a strong scrambling method.
        return new BCryptPasswordEncoder();
    }

    /**
     * Sets up all the security checkpoints for web requests.
     *
     * @param http Web security builder.
     * @return The full set of security rules.
     * @throws Exception If rules can't be set up.
     */
    @Bean // Spring creates and manages these rules.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Step 1: Who can access which pages?
                .authorizeHttpRequests(authorize -> authorize
                        // Everyone can access these pages/files.
                        .requestMatchers("/", "/home", "/register", "/css/**").permitAll()
                        // Must be logged in to see info list.
                        .requestMatchers("/information/list").authenticated()
                        // Full Users or Admins can add/edit/delete info.
                        .requestMatchers("/information/add", "/information/edit/**", "/information/delete/**").hasAnyRole("FULL_USER", "ADMIN")
                        // Only Admins can access admin pages.
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // All other pages require login.
                        .anyRequest().authenticated()
                )
                // Step 2: How users log in.
                .formLogin(form -> form
                        // Custom login page.
                        .loginPage("/login")
                        // Login page is public.
                        .permitAll()
                        // Go to dashboard after login.
                        .defaultSuccessUrl("/dashboard", true)
                        // Go back to login on failure.
                        .failureUrl("/login?error")
                )
                // Step 3: How users log out.
                .logout(logout -> logout
                        // Logout URL.
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        // Go to login page after logout.
                        .logoutSuccessUrl("/login?logout")
                        // End user's session.
                        .invalidateHttpSession(true)
                        // Remove session cookies.
                        .deleteCookies("JSESSIONID")
                        // Anyone can trigger logout.
                        .permitAll()
                )
                // Step 4: Protects against web attacks (CSRF).
                // Ignores this protection for the H2 database console.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                // Step 5: Protects pages from being embedded maliciously.
                // Allows embedding only from the same website.
                .headers(headers -> headers.frameOptions().sameOrigin());

        // Finalize and return the security rules.
        return http.build();
    }
}

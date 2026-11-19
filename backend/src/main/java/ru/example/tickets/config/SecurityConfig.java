package ru.example.tickets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    InMemoryUserDetailsManager users(Environment env, BCryptPasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("user").password(encoder.encode(env.getProperty("USER_PASSWORD", "user-lab2"))).roles("USER").build(),
            User.withUsername("user2").password(encoder.encode(env.getProperty("USER2_PASSWORD", "user2-lab2"))).roles("USER").build(),
            User.withUsername("admin").password(encoder.encode(env.getProperty("ADMIN_PASSWORD", "admin-lab2"))).roles("ADMIN").build(),
            User.withUsername("admin2").password(encoder.encode(env.getProperty("ADMIN2_PASSWORD", "admin2-lab2"))).roles("ADMIN").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a -> a.requestMatchers("/api/**").authenticated().anyRequest().permitAll())
            .httpBasic(b -> b.authenticationEntryPoint((request, response, error) -> {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Требуется вход: проверьте логин и пароль\"}");
            })).build();
    }
}

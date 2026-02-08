package com.example.project2.config;

import com.example.project2.repository.AppUserRepository;
import com.example.project2.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    JwtService jwtService() {
        return new JwtService();
    }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository repository) {
        return email -> repository.findByEmail(email)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AppUserRepository repository) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/styles.css", "/", "/index", "/error", "/login", "/register").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/properties/**").permitAll()
                        .requestMatchers("/deals").authenticated()
                        .requestMatchers("/deal/**").permitAll()
                        .requestMatchers("/chat").authenticated()
                        .requestMatchers("/v1/api/auth/**").permitAll()
                        .requestMatchers("/v1/api/uploads/**").authenticated()
                        // Разрешаем GET запросы к properties для всех (более специфичное правило должно быть первым)
                        .requestMatchers("GET", "/v1/api/properties", "/v1/api/properties/*").permitAll()
                        // Все остальные запросы к properties требуют аутентификации
                        .requestMatchers("/v1/api/properties/**").authenticated()
                        .requestMatchers("/v1/api/realtor/**").hasAnyRole("ADMIN", "REALTOR")
                        .requestMatchers("/v1/api/chat/**").authenticated()
                        .requestMatchers("/v1/api/favorites/my-ids").permitAll()
                        .requestMatchers("/v1/api/favorites/**").authenticated()
                        .requestMatchers("/v1/api/view-history/**").authenticated()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/realtor/**").hasAnyRole("ADMIN", "REALTOR")
                        .requestMatchers("/realtor/stats").hasAnyRole("ADMIN", "REALTOR")
                        .requestMatchers("/user/favorites").permitAll()
                        .requestMatchers("/user/profile").hasAnyRole("ADMIN", "REALTOR", "USER")
                        .requestMatchers("/user/**").hasAnyRole("ADMIN", "REALTOR", "USER")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login").permitAll()
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/").permitAll()
                )
                .sessionManagement(session -> session
                        .invalidSessionUrl("/")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .and()
                        .sessionFixation().migrateSession()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService(), userDetailsService(repository)), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}



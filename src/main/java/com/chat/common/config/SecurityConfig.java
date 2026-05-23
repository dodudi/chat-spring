package com.chat.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityErrorResponder securityErrorResponder;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/ws/chat",
            "/h2-console/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, ex) ->
                                securityErrorResponder.writeUnauthorized(response))
                        .accessDeniedHandler((request, response, ex) ->
                                securityErrorResponder.writeForbidden(response))
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        boolean hasWildcard = false;
        for (String origin : allowedOrigins.split(",")) {
            String trimmed = origin.trim();
            config.addAllowedOriginPattern(trimmed);
            if ("*".equals(trimmed)) {
                hasWildcard = true;
            }
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        // wildcard origin과 credentials는 함께 사용 불가 (브라우저 차단)
        if (!hasWildcard) {
            config.setAllowCredentials(true);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

package com.chat.common.config;

import com.chat.common.ApiResponse;
import com.chat.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    private static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/ws/chat",      // WebSocket upgrade — JWT auth at STOMP CONNECT level
            "/h2-console/**" // H2 console (local only — spring.h2.console.enabled=false in prod)
    };

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())) // H2 콘솔 iframe 허용
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                        .authenticationEntryPoint((request, response, ex) -> {
                            log.warn("[AUTH_FAIL] uri={}", request.getRequestURI());
                            writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            log.warn("[ACCESS_DENIED] uri={}", request.getRequestURI());
                            writeErrorResponse(response, ErrorCode.FORBIDDEN);
                        }))
                .build();
    }

    private static void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }
}

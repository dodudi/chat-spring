package com.chat.websocket.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import java.security.Principal;

public record JwtPrincipal(Jwt jwt) implements Principal {

    @Override
    public String getName() {
        return jwt.getSubject();
    }
}

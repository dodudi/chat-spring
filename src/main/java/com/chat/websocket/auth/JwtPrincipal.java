package com.chat.websocket.auth;

import java.security.Principal;

public record JwtPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}

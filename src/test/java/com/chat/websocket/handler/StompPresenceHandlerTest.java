package com.chat.websocket.handler;

import com.chat.websocket.presence.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StompPresenceHandlerTest {

    @Mock PresenceService presenceService;
    @InjectMocks StompPresenceHandler handler;

    @Test
    void heartbeat_presenceService에_위임() {
        // given
        Principal principal = () -> "user-a";

        // when
        handler.heartbeat(principal);

        // then
        then(presenceService).should().heartbeat("user-a");
    }

    @Test
    void onDisconnect_연결_종료시_오프라인_처리() {
        // given
        Principal principal = () -> "user-a";
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        given(event.getUser()).willReturn(principal);

        // when
        handler.onDisconnect(event);

        // then
        then(presenceService).should().offline("user-a");
    }

    @Test
    void onDisconnect_인증_전_연결_종료시_무시() {
        // given — CONNECT 전에 끊긴 경우 getUser()가 null
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        given(event.getUser()).willReturn(null);

        // when
        handler.onDisconnect(event);

        // then
        then(presenceService).shouldHaveNoInteractions();
    }
}

package com.chat.websocket.handler;

import com.chat.common.exception.AppException;
import com.chat.websocket.dto.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class StompExceptionAdvice {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(AppException.class)
    public void handleAppException(AppException ex, Principal principal) {
        log.warn("[WS_APP_EXCEPTION] user={} code={}", principal.getName(), ex.getErrorCode().getCode());
        messagingTemplate.convertAndSendToUser(
                principal.getName(), "/queue/errors", ErrorMessage.from(ex));
    }

    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception ex, Principal principal) {
        log.error("[WS_EXCEPTION] user={}", principal.getName(), ex);
        messagingTemplate.convertAndSendToUser(
                principal.getName(), "/queue/errors",
                ErrorMessage.of("C003", "서버 내부 오류가 발생했습니다."));
    }
}

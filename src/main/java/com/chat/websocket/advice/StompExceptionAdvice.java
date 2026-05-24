package com.chat.websocket.advice;

import com.chat.common.ApiResponse;
import com.chat.common.exception.AppException;
import com.chat.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class StompExceptionAdvice {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(AppException.class)
    public void handleAppException(AppException e, Principal principal) {
        log.warn("[STOMP_ERROR] userId={} code={}", principal.getName(), e.getErrorCode().getCode());
        messagingTemplate.convertAndSendToUser(
                principal.getName(), "/queue/errors",
                ApiResponse.error(e.getErrorCode().getCode(), e.getErrorCode().getMessage()));
    }

    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception e, Principal principal) {
        String userId = principal != null ? principal.getName() : "unknown";
        log.error("[STOMP_UNHANDLED] userId={}", userId, e);
        messagingTemplate.convertAndSendToUser(
                userId, "/queue/errors",
                ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}

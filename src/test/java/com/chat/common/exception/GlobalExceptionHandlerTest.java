package com.chat.common.exception;

import com.chat.common.ApiResponse;
import com.chat.message.application.MessageService;
import com.chat.room.application.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.chat.websocket.presence.PresenceService;
import com.chat.websocket.redis.ChatMessagePublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @MockitoBean
    private ChatMessagePublisher chatMessagePublisher;

    @MockitoBean
    private PresenceService presenceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser
    void AppException_발생시_ErrorCode의_HTTP상태와_코드로_응답() throws Exception {
        mockMvc.perform(get("/test/app-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @WithMockUser
    void MethodArgumentNotValidException_발생시_C001과_필드_오류메시지로_응답() throws Exception {
        // given
        TestController.TestRequest request = new TestController.TestRequest("");

        // when & then
        mockMvc.perform(post("/test/validation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @WithMockUser
    void 미등록_경로_요청시_C002로_응답() throws Exception {
        mockMvc.perform(get("/test/not-exist-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("C002"));
    }

    @Test
    @WithMockUser
    void 예상치_못한_예외_발생시_C003으로_응답() throws Exception {
        mockMvc.perform(get("/test/unexpected-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("C003"));
    }

    @RestController
    @RequestMapping("/test")
    @Validated
    static class TestController {

        @GetMapping("/app-exception")
        public ResponseEntity<ApiResponse<Void>> appException() {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        @PostMapping("/validation")
        public ResponseEntity<ApiResponse<Void>> validation(@Valid @RequestBody TestRequest request) {
            return ResponseEntity.ok(ApiResponse.fail(ErrorCode.INVALID_INPUT));
        }

        @GetMapping("/unexpected-exception")
        public ResponseEntity<ApiResponse<Void>> unexpectedException() {
            throw new RuntimeException("예상치 못한 오류");
        }

        record TestRequest(@NotBlank String name) {}
    }
}

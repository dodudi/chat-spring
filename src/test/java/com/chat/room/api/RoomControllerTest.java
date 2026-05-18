package com.chat.room.api;

import com.chat.room.application.RoomService;
import com.chat.room.domain.RoomType;
import com.chat.room.dto.CreateDmRoomRequest;
import com.chat.room.dto.CreateGroupRoomRequest;
import com.chat.room.dto.InviteMemberRequest;
import com.chat.room.dto.RoomSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RoomService roomService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean SimpMessagingTemplate simpMessagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RoomSummaryResponse dmSummary() {
        return new RoomSummaryResponse(1L, RoomType.DM, null, "user-b",
                null, null, 0L, OffsetDateTime.now());
    }

    private RoomSummaryResponse groupSummary() {
        return new RoomSummaryResponse(2L, RoomType.GROUP, "팀 채팅", null,
                null, null, 0L, OffsetDateTime.now());
    }

    @Test
    void createOrGetDmRoom_200_반환() throws Exception {
        // given
        given(roomService.createOrGetDmRoom(any(), any(CreateDmRoomRequest.class))).willReturn(dmSummary());
        String body = objectMapper.writeValueAsString(new CreateDmRoomRequest("user-b"));

        // when & then
        mockMvc.perform(post("/api/v1/rooms/direct")
                        .with(jwt().jwt(j -> j.subject("user-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("DM"))
                .andExpect(jsonPath("$.data.dmTargetUserId").value("user-b"));
    }

    @Test
    void createOrGetDmRoom_targetUserId_없으면_400_반환() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(new CreateDmRoomRequest(""));

        // when & then
        mockMvc.perform(post("/api/v1/rooms/direct")
                        .with(jwt().jwt(j -> j.subject("user-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGroupRoom_201_반환() throws Exception {
        // given
        given(roomService.createGroupRoom(any(), any(CreateGroupRoomRequest.class))).willReturn(groupSummary());
        String body = objectMapper.writeValueAsString(
                new CreateGroupRoomRequest("팀 채팅", List.of("user-b")));

        // when & then
        mockMvc.perform(post("/api/v1/rooms/group")
                        .with(jwt().jwt(j -> j.subject("user-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/rooms/2"))
                .andExpect(jsonPath("$.data.type").value("GROUP"));
    }

    @Test
    void getMyRooms_200_반환() throws Exception {
        // given
        given(roomService.getMyRooms(any())).willReturn(List.of(dmSummary(), groupSummary()));

        // when & then
        mockMvc.perform(get("/api/v1/rooms")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void inviteMembers_200_반환() throws Exception {
        // given
        willDoNothing().given(roomService).inviteMembers(any(), eq(1L), any(InviteMemberRequest.class));
        String body = objectMapper.writeValueAsString(new InviteMemberRequest(List.of("user-c")));

        // when & then
        mockMvc.perform(post("/api/v1/rooms/1/members")
                        .with(jwt().jwt(j -> j.subject("user-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void leaveRoom_204_반환() throws Exception {
        // given
        willDoNothing().given(roomService).leaveRoom(any(), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/rooms/1/members/me")
                        .with(jwt().jwt(j -> j.subject("user-a"))))
                .andExpect(status().isNoContent());
    }
}

package kr.it.rudy.chat.friend.api;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.friend.application.FriendService;
import kr.it.rudy.chat.friend.domain.FriendRequestStatus;
import kr.it.rudy.chat.friend.dto.FriendRequestResponse;
import kr.it.rudy.chat.friend.dto.FriendshipResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendApiController.class)
class FriendApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void sendRequest_정상_요청시_201_반환() throws Exception {
        // given
        FriendRequestResponse response = new FriendRequestResponse(
                10L, 1L, "ext-requester", 2L, "ext-receiver", FriendRequestStatus.PENDING, null);
        given(friendService.sendRequest(eq("ext-requester"), eq(2L))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/friends/requests")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-requester")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/friends/requests/10"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requesterId").value(1));
    }

    @Test
    void sendRequest_receiverId_누락시_400_반환() throws Exception {
        mockMvc.perform(post("/api/v1/friends/requests")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendRequest_인증_없으면_401_반환() throws Exception {
        mockMvc.perform(post("/api/v1/friends/requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendRequest_이미_요청이_있으면_409_반환() throws Exception {
        // given
        given(friendService.sendRequest(any(), eq(2L)))
                .willThrow(new AuthException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post("/api/v1/friends/requests")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FR002"));
    }

    @Test
    void getReceivedRequests_200_반환() throws Exception {
        // given
        List<FriendRequestResponse> responses = List.of(
                new FriendRequestResponse(10L, 1L, "ext-requester", 2L, "ext-receiver", FriendRequestStatus.PENDING, null)
        );
        given(friendService.getReceivedRequests("ext-receiver")).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/friends/requests/received")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-receiver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].requesterId").value(1));
    }

    @Test
    void getSentRequests_200_반환() throws Exception {
        // given
        List<FriendRequestResponse> responses = List.of(
                new FriendRequestResponse(10L, 1L, "ext-requester", 2L, "ext-receiver", FriendRequestStatus.PENDING, null)
        );
        given(friendService.getSentRequests("ext-requester")).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/friends/requests/sent")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-requester"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].receiverId").value(2));
    }

    @Test
    void acceptRequest_200_반환() throws Exception {
        // given
        FriendRequestResponse response = new FriendRequestResponse(
                10L, 1L, "ext-requester", 2L, "ext-receiver", FriendRequestStatus.ACCEPTED, null);
        given(friendService.acceptRequest(eq("ext-receiver"), eq(10L))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/friends/requests/10/accept")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-receiver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void acceptRequest_권한_없으면_403_반환() throws Exception {
        // given
        given(friendService.acceptRequest(any(), eq(10L)))
                .willThrow(new AuthException(ErrorCode.FRIEND_REQUEST_FORBIDDEN));

        // when & then
        mockMvc.perform(post("/api/v1/friends/requests/10/accept")
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FR005"));
    }

    @Test
    void rejectRequest_200_반환() throws Exception {
        // given
        FriendRequestResponse response = new FriendRequestResponse(
                10L, 1L, "ext-requester", 2L, "ext-receiver", FriendRequestStatus.REJECTED, null);
        given(friendService.rejectRequest(eq("ext-receiver"), eq(10L))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/friends/requests/10/reject")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-receiver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void cancelRequest_204_반환() throws Exception {
        // given
        willDoNothing().given(friendService).cancelRequest(eq("ext-requester"), eq(10L));

        // when & then
        mockMvc.perform(delete("/api/v1/friends/requests/10")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-requester"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFriends_200_반환() throws Exception {
        // given
        List<FriendshipResponse> responses = List.of(
                new FriendshipResponse(20L, 2L, "ext-receiver", null)
        );
        given(friendService.getFriends("ext-requester")).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/friends")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-requester"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].friendId").value(2))
                .andExpect(jsonPath("$.data[0].friendExternalId").value("ext-receiver"));
    }

    @Test
    void removeFriend_204_반환() throws Exception {
        // given
        willDoNothing().given(friendService).removeFriend(eq("ext-requester"), eq(2L));

        // when & then
        mockMvc.perform(delete("/api/v1/friends/2")
                        .with(jwt().jwt(j -> j.claim("sub", "ext-requester"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFriend_친구_관계가_없으면_404_반환() throws Exception {
        // given
        willThrow(new AuthException(ErrorCode.FRIENDSHIP_NOT_FOUND))
                .given(friendService).removeFriend(any(), eq(999L));

        // when & then
        mockMvc.perform(delete("/api/v1/friends/999")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FR006"));
    }
}

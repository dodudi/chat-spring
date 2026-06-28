package kr.it.rudy.chat.friend.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.friend.domain.*;
import kr.it.rudy.chat.friend.dto.FriendRequestResponse;
import kr.it.rudy.chat.friend.dto.FriendshipResponse;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SimpleFriendServiceTest {

    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SimpleFriendService friendService;

    private User requester;
    private User receiver;
    private FriendRequest friendRequest;

    @BeforeEach
    void setUp() {
        requester = User.create("ext-requester");
        ReflectionTestUtils.setField(requester, "id", 1L);

        receiver = User.create("ext-receiver");
        ReflectionTestUtils.setField(receiver, "id", 2L);

        friendRequest = FriendRequest.create(requester, receiver);
        ReflectionTestUtils.setField(friendRequest, "id", 10L);
    }

    @Test
    void sendRequest_정상_요청시_FriendRequestResponse_반환() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(1L, 2L, FriendRequestStatus.PENDING)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(2L, 1L, FriendRequestStatus.PENDING)).willReturn(false);
        given(friendshipRepository.existsByUserIdAndFriendId(1L, 2L)).willReturn(false);
        given(friendRequestRepository.save(any())).willReturn(friendRequest);

        // when
        FriendRequestResponse response = friendService.sendRequest("ext-requester", 2L);

        // then
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(FriendRequestStatus.PENDING);
        assertThat(response.requesterId()).isEqualTo(1L);
        assertThat(response.receiverId()).isEqualTo(2L);
    }

    @Test
    void sendRequest_자기_자신에게_요청시_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(userRepository.findById(1L)).willReturn(Optional.of(requester));

        // when & then
        assertThatThrownBy(() -> friendService.sendRequest("ext-requester", 1L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.SELF_FRIEND_REQUEST.getMessage());
    }

    @Test
    void sendRequest_이미_PENDING_요청이_있으면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(1L, 2L, FriendRequestStatus.PENDING)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> friendService.sendRequest("ext-requester", 2L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS.getMessage());
    }

    @Test
    void sendRequest_이미_친구이면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(1L, 2L, FriendRequestStatus.PENDING)).willReturn(false);
        given(friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(2L, 1L, FriendRequestStatus.PENDING)).willReturn(false);
        given(friendshipRepository.existsByUserIdAndFriendId(1L, 2L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> friendService.sendRequest("ext-requester", 2L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.ALREADY_FRIENDS.getMessage());
    }

    @Test
    void getReceivedRequests_받은_PENDING_요청_목록_반환() {
        // given
        given(userRepository.findByExternalId("ext-receiver")).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findByReceiverIdAndStatus(2L, FriendRequestStatus.PENDING))
                .willReturn(List.of(friendRequest));

        // when
        List<FriendRequestResponse> responses = friendService.getReceivedRequests("ext-receiver");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).requesterId()).isEqualTo(1L);
    }

    @Test
    void getSentRequests_보낸_PENDING_요청_목록_반환() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(friendRequestRepository.findByRequesterIdAndStatus(1L, FriendRequestStatus.PENDING))
                .willReturn(List.of(friendRequest));

        // when
        List<FriendRequestResponse> responses = friendService.getSentRequests("ext-requester");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).receiverId()).isEqualTo(2L);
    }

    @Test
    void acceptRequest_정상_수락시_ACCEPTED_상태_반환() {
        // given
        given(userRepository.findByExternalId("ext-receiver")).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findById(10L)).willReturn(Optional.of(friendRequest));
        given(friendshipRepository.save(any())).willReturn(null);

        // when
        FriendRequestResponse response = friendService.acceptRequest("ext-receiver", 10L);

        // then
        assertThat(response.status()).isEqualTo(FriendRequestStatus.ACCEPTED);
        then(friendshipRepository).should().save(any(Friendship.class));
    }

    @Test
    void acceptRequest_수신자가_아니면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(friendRequestRepository.findById(10L)).willReturn(Optional.of(friendRequest));

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest("ext-requester", 10L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.FRIEND_REQUEST_FORBIDDEN.getMessage());
    }

    @Test
    void rejectRequest_정상_거절시_REJECTED_상태_반환() {
        // given
        given(userRepository.findByExternalId("ext-receiver")).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findById(10L)).willReturn(Optional.of(friendRequest));

        // when
        FriendRequestResponse response = friendService.rejectRequest("ext-receiver", 10L);

        // then
        assertThat(response.status()).isEqualTo(FriendRequestStatus.REJECTED);
    }

    @Test
    void cancelRequest_정상_취소시_CANCELLED_상태() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(friendRequestRepository.findById(10L)).willReturn(Optional.of(friendRequest));

        // when
        friendService.cancelRequest("ext-requester", 10L);

        // then
        assertThat(friendRequest.getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
    }

    @Test
    void cancelRequest_발신자가_아니면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-receiver")).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findById(10L)).willReturn(Optional.of(friendRequest));

        // when & then
        assertThatThrownBy(() -> friendService.cancelRequest("ext-receiver", 10L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.FRIEND_REQUEST_FORBIDDEN.getMessage());
    }

    @Test
    void getFriends_친구_목록_반환() {
        // given
        Friendship friendship = Friendship.create(requester, receiver);
        ReflectionTestUtils.setField(friendship, "id", 20L);

        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(friendshipRepository.findAllByUserId(1L)).willReturn(List.of(friendship));

        // when
        List<FriendshipResponse> responses = friendService.getFriends("ext-requester");

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).friendId()).isEqualTo(2L);
        assertThat(responses.get(0).friendExternalId()).isEqualTo("ext-receiver");
    }

    @Test
    void removeFriend_정상_삭제시_friendship_삭제() {
        // given
        Friendship friendship = Friendship.create(requester, receiver);

        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendshipRepository.findByUserIds(1L, 2L)).willReturn(Optional.of(friendship));

        // when
        friendService.removeFriend("ext-requester", 2L);

        // then
        then(friendshipRepository).should().delete(friendship);
    }

    @Test
    void removeFriend_친구_관계가_없으면_AuthException_발생() {
        // given
        given(userRepository.findByExternalId("ext-requester")).willReturn(Optional.of(requester));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(friendshipRepository.findByUserIds(1L, 2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> friendService.removeFriend("ext-requester", 2L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(ErrorCode.FRIENDSHIP_NOT_FOUND.getMessage());
    }
}

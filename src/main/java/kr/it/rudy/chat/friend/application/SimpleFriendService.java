package kr.it.rudy.chat.friend.application;

import kr.it.rudy.chat.common.exception.AuthException;
import kr.it.rudy.chat.common.exception.ErrorCode;
import kr.it.rudy.chat.friend.domain.*;
import kr.it.rudy.chat.friend.dto.FriendRequestResponse;
import kr.it.rudy.chat.friend.dto.FriendshipResponse;
import kr.it.rudy.chat.user.domain.User;
import kr.it.rudy.chat.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SimpleFriendService implements FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FriendRequestResponse sendRequest(String externalId, Long receiverId) {
        User requester = findUser(externalId);
        User receiver = findUserById(receiverId);
        if (requester.getId().equals(receiver.getId())) {
            throw new AuthException(ErrorCode.SELF_FRIEND_REQUEST);
        }
        if (friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(requester.getId(), receiver.getId(), FriendRequestStatus.PENDING)
                || friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(receiver.getId(), requester.getId(), FriendRequestStatus.PENDING)) {
            throw new AuthException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }
        Long minId = Math.min(requester.getId(), receiver.getId());
        Long maxId = Math.max(requester.getId(), receiver.getId());
        if (friendshipRepository.existsByUserIdAndFriendId(minId, maxId)) {
            throw new AuthException(ErrorCode.ALREADY_FRIENDS);
        }
        FriendRequest request = friendRequestRepository.save(FriendRequest.create(requester, receiver));
        log.info("[FRIEND_REQUEST_SEND] requesterId={} receiverId={}", requester.getId(), receiverId);
        return FriendRequestResponse.from(request);
    }

    @Override
    public List<FriendRequestResponse> getReceivedRequests(String externalId) {
        User user = findUser(externalId);
        return friendRequestRepository.findByReceiverIdAndStatus(user.getId(), FriendRequestStatus.PENDING)
                .stream().map(FriendRequestResponse::from).toList();
    }

    @Override
    public List<FriendRequestResponse> getSentRequests(String externalId) {
        User user = findUser(externalId);
        return friendRequestRepository.findByRequesterIdAndStatus(user.getId(), FriendRequestStatus.PENDING)
                .stream().map(FriendRequestResponse::from).toList();
    }

    @Override
    @Transactional
    public FriendRequestResponse acceptRequest(String externalId, Long requestId) {
        User user = findUser(externalId);
        FriendRequest request = findRequest(requestId);
        if (!request.isReceivedBy(user)) {
            throw new AuthException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }
        request.accept();
        friendshipRepository.save(Friendship.create(request.getRequester(), request.getReceiver()));
        log.info("[FRIEND_REQUEST_ACCEPT] requestId={} userId={}", requestId, user.getId());
        return FriendRequestResponse.from(request);
    }

    @Override
    @Transactional
    public FriendRequestResponse rejectRequest(String externalId, Long requestId) {
        User user = findUser(externalId);
        FriendRequest request = findRequest(requestId);
        if (!request.isReceivedBy(user)) {
            throw new AuthException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }
        request.reject();
        log.info("[FRIEND_REQUEST_REJECT] requestId={} userId={}", requestId, user.getId());
        return FriendRequestResponse.from(request);
    }

    @Override
    @Transactional
    public void cancelRequest(String externalId, Long requestId) {
        User user = findUser(externalId);
        FriendRequest request = findRequest(requestId);
        if (!request.isSentBy(user)) {
            throw new AuthException(ErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }
        request.cancel();
        log.info("[FRIEND_REQUEST_CANCEL] requestId={} userId={}", requestId, user.getId());
    }

    @Override
    public List<FriendshipResponse> getFriends(String externalId) {
        User user = findUser(externalId);
        return friendshipRepository.findAllByUserId(user.getId())
                .stream().map(f -> FriendshipResponse.of(f, user.getId())).toList();
    }

    @Override
    @Transactional
    public void removeFriend(String externalId, Long friendId) {
        User user = findUser(externalId);
        User friend = findUserById(friendId);
        Long minId = Math.min(user.getId(), friend.getId());
        Long maxId = Math.max(user.getId(), friend.getId());
        Friendship friendship = friendshipRepository.findByUserIds(minId, maxId)
                .orElseThrow(() -> new AuthException(ErrorCode.FRIENDSHIP_NOT_FOUND));
        friendshipRepository.delete(friendship);
        log.info("[FRIEND_REMOVE] userId={} friendId={}", user.getId(), friendId);
    }

    private User findUser(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private FriendRequest findRequest(Long requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new AuthException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
    }
}

package kr.it.rudy.chat.friend.application;

import kr.it.rudy.chat.friend.dto.FriendRequestResponse;
import kr.it.rudy.chat.friend.dto.FriendshipResponse;

import java.util.List;

public interface FriendService {
    FriendRequestResponse sendRequest(String externalId, Long receiverId);

    List<FriendRequestResponse> getReceivedRequests(String externalId);

    List<FriendRequestResponse> getSentRequests(String externalId);

    FriendRequestResponse acceptRequest(String externalId, Long requestId);

    FriendRequestResponse rejectRequest(String externalId, Long requestId);

    void cancelRequest(String externalId, Long requestId);

    List<FriendshipResponse> getFriends(String externalId);

    void removeFriend(String externalId, Long friendId);
}

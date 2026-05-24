package com.chat.group.application;

import com.chat.group.dto.CreateGroupRequest;
import com.chat.group.dto.GroupResponse;
import com.chat.group.dto.UpdateGroupRequest;

public interface GroupManager {

    /** 예외: G002(이름 중복), G003(최대 개수 초과) */
    GroupResponse createGroup(String userId, CreateGroupRequest request);

    /** 예외: G001(없음), G004(기본 그룹), G002(이름 중복) */
    GroupResponse renameGroup(String userId, Long groupId, UpdateGroupRequest request);

    /** 예외: G001(없음), G004(기본 그룹). 연결된 채팅방은 그룹 연결만 제거되고 참여는 유지 */
    void deleteGroup(String userId, Long groupId);
}

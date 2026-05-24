package com.chat.group.application;

import com.chat.group.dto.GroupResponse;

import java.util.List;

public interface GroupReader {

    List<GroupResponse> getMyGroups(String userId);
}

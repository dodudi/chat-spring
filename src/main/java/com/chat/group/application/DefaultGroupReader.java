package com.chat.group.application;

import com.chat.group.dto.GroupResponse;
import com.chat.group.infrastructure.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultGroupReader implements GroupReader {

    private final UserGroupRepository userGroupRepository;

    @Override
    public List<GroupResponse> getMyGroups(String userId) {
        return userGroupRepository.findAllByUserId(userId).stream()
                .map(GroupResponse::from)
                .toList();
    }
}

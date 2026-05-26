package com.chat.group.application;

import com.chat.group.domain.UserGroup;
import com.chat.group.dto.GroupResponse;
import com.chat.group.infrastructure.UserGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DefaultGroupReaderTest {

    @InjectMocks
    private DefaultGroupReader groupReader;

    @Mock private UserGroupRepository userGroupRepository;

    @Test
    @DisplayName("내 그룹 목록 조회 성공")
    void getMyGroups_성공() {
        // given
        String userId = "user-1";
        UserGroup defaultGroup = UserGroup.createDefault(userId);
        UserGroup customGroup = UserGroup.create(userId, "스터디");

        given(userGroupRepository.findAllByUserId(userId)).willReturn(List.of(defaultGroup, customGroup));

        // when
        List<GroupResponse> responses = groupReader.getMyGroups(userId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("전체");
        assertThat(responses.get(1).name()).isEqualTo("스터디");
    }

    @Test
    @DisplayName("그룹이 없으면 빈 목록 반환")
    void getMyGroups_빈목록() {
        // given
        String userId = "user-1";
        given(userGroupRepository.findAllByUserId(userId)).willReturn(List.of());

        // when
        List<GroupResponse> responses = groupReader.getMyGroups(userId);

        // then
        assertThat(responses).isEmpty();
    }
}

package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.RoomType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ChatRoomRepositoryTest {

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Test
    void findByDmUserAAndDmUserBAndType_기존_DM방_조회시_반환() {
        // given
        ChatRoom room = ChatRoom.createDm("user-a", "user-b");
        chatRoomRepository.save(room);

        // when
        Optional<ChatRoom> result = chatRoomRepository.findByDmUserAAndDmUserBAndType(
                "user-a", "user-b", RoomType.DM);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(RoomType.DM);
    }

    @Test
    void findByDmUserAAndDmUserBAndType_없는_DM방_조회시_빈값_반환() {
        // given - 방 없음

        // when
        Optional<ChatRoom> result = chatRoomRepository.findByDmUserAAndDmUserBAndType(
                "user-x", "user-y", RoomType.DM);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findByDmUserAAndDmUserBAndType_정렬된_순서로_저장되므로_역순_조회_불가() {
        // given — createDm은 항상 사전순 정렬로 저장
        ChatRoom room = ChatRoom.createDm("user-b", "user-a"); // 내부적으로 a, b 순 저장
        chatRoomRepository.save(room);

        // when — 정렬된 순서로 조회해야 찾을 수 있음
        Optional<ChatRoom> withSortedOrder = chatRoomRepository.findByDmUserAAndDmUserBAndType(
                "user-a", "user-b", RoomType.DM);
        Optional<ChatRoom> withReverseOrder = chatRoomRepository.findByDmUserAAndDmUserBAndType(
                "user-b", "user-a", RoomType.DM);

        // then
        assertThat(withSortedOrder).isPresent();
        assertThat(withReverseOrder).isEmpty();
    }
}

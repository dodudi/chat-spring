package com.chat.room.infrastructure;

import com.chat.room.domain.ChatRoom;
import com.chat.room.domain.ChatRoomMember;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ChatRoomMemberRepositoryTest {

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    EntityManager entityManager;

    private ChatRoom room;
    private ChatRoomMember member;

    @BeforeEach
    void setUp() {
        room = chatRoomRepository.save(ChatRoom.createGroup("user-a", "테스트 방"));
        member = chatRoomMemberRepository.save(ChatRoomMember.create(room, "user-a"));
    }

    @Test
    void findByRoom_IdAndUserId_멤버가_존재하면_반환() {
        // when
        Optional<ChatRoomMember> result = chatRoomMemberRepository
                .findByRoom_IdAndUserId(room.getId(), "user-a");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo("user-a");
    }

    @Test
    void findByRoom_IdAndUserId_멤버가_없으면_빈값_반환() {
        // when
        Optional<ChatRoomMember> result = chatRoomMemberRepository
                .findByRoom_IdAndUserId(room.getId(), "user-z");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void existsByRoom_IdAndUserId_멤버가_있으면_true() {
        assertThat(chatRoomMemberRepository.existsByRoom_IdAndUserId(room.getId(), "user-a")).isTrue();
    }

    @Test
    void existsByRoom_IdAndUserId_멤버가_없으면_false() {
        assertThat(chatRoomMemberRepository.existsByRoom_IdAndUserId(room.getId(), "user-z")).isFalse();
    }

    @Test
    void findByRoom_IdAndActiveTrue_비활성_멤버는_제외() {
        // given
        ChatRoomMember inactive = chatRoomMemberRepository.save(ChatRoomMember.create(room, "user-b"));
        inactive.leave();
        chatRoomMemberRepository.save(inactive);

        // when
        List<ChatRoomMember> active = chatRoomMemberRepository.findByRoom_IdAndActiveTrue(room.getId());

        // then — user-a만 활성
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getUserId()).isEqualTo("user-a");
    }

    @Test
    void findMyRoomsWithUnread_활성_방_목록_반환() {
        // given
        entityManager.flush();

        // when
        List<RoomSummaryProjection> rooms = chatRoomMemberRepository.findMyRoomsWithUnread("user-a");

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getId()).isEqualTo(room.getId());
        assertThat(rooms.get(0).getUnreadCount()).isZero();
        assertThat(rooms.get(0).getLastMessageContent()).isNull();
    }

    @Test
    void findMyRoomsWithUnread_비활성_방은_제외() {
        // given — member가 방을 나감
        member.leave();
        chatRoomMemberRepository.save(member);
        entityManager.flush();

        // when
        List<RoomSummaryProjection> rooms = chatRoomMemberRepository.findMyRoomsWithUnread("user-a");

        // then
        assertThat(rooms).isEmpty();
    }
}

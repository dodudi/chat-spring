package com.chat.message.infrastructure;

import com.chat.message.domain.Message;
import com.chat.room.domain.ChatRoom;
import com.chat.room.infrastructure.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MessageRepositoryTest {

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    private Long roomId;

    @BeforeEach
    void setUp() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.createGroup("user-a", "테스트 방"));
        roomId = room.getId();
    }

    @Test
    void findByRoomIdOrderByIdDesc_최신순으로_반환() {
        // given
        Message m1 = messageRepository.save(Message.create(roomId, "user-a", "첫 번째"));
        Message m2 = messageRepository.save(Message.create(roomId, "user-a", "두 번째"));
        Message m3 = messageRepository.save(Message.create(roomId, "user-a", "세 번째"));

        // when
        List<Message> result = messageRepository.findByRoomIdOrderByIdDesc(
                roomId, PageRequest.of(0, 10));

        // then — 최신순 (m3 → m2 → m1)
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(m3.getId());
        assertThat(result.get(2).getId()).isEqualTo(m1.getId());
    }

    @Test
    void findByRoomIdAndIdLessThanOrderByIdDesc_커서_기반_페이징() {
        // given
        Message m1 = messageRepository.save(Message.create(roomId, "user-a", "첫 번째"));
        Message m2 = messageRepository.save(Message.create(roomId, "user-a", "두 번째"));
        Message m3 = messageRepository.save(Message.create(roomId, "user-a", "세 번째"));

        // when — m3 이전 메시지 2개 조회 (before = m3.id)
        List<Message> result = messageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(
                roomId, m3.getId(), PageRequest.of(0, 2));

        // then — m2, m1
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(m2.getId());
        assertThat(result.get(1).getId()).isEqualTo(m1.getId());
    }

    @Test
    void findByRoomIdOrderByIdDesc_size_만큼만_반환() {
        // given
        messageRepository.save(Message.create(roomId, "user-a", "1"));
        messageRepository.save(Message.create(roomId, "user-a", "2"));
        messageRepository.save(Message.create(roomId, "user-a", "3"));

        // when — size=2
        List<Message> result = messageRepository.findByRoomIdOrderByIdDesc(
                roomId, PageRequest.of(0, 2));

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void SQLRestriction_삭제된_메시지는_조회_제외() {
        // given
        Message m1 = messageRepository.save(Message.create(roomId, "user-a", "살아있음"));
        Message m2 = messageRepository.save(Message.create(roomId, "user-a", "삭제됨"));
        m2.delete();
        messageRepository.save(m2);
        messageRepository.flush();

        // when
        List<Message> result = messageRepository.findByRoomIdOrderByIdDesc(
                roomId, PageRequest.of(0, 10));

        // then — 삭제된 m2는 제외
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(m1.getId());
    }
}

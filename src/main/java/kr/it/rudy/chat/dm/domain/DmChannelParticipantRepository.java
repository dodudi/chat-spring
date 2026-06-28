package kr.it.rudy.chat.dm.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DmChannelParticipantRepository extends JpaRepository<DmChannelParticipant, Long> {

    List<DmChannelParticipant> findByUserIdAndLeftAtIsNull(Long userId);

    Optional<DmChannelParticipant> findByDmChannelIdAndUserIdAndLeftAtIsNull(Long dmChannelId, Long userId);

    Optional<DmChannelParticipant> findByDmChannelIdAndUserId(Long dmChannelId, Long userId);

    boolean existsByDmChannelIdAndUserIdAndLeftAtIsNull(Long dmChannelId, Long userId);

    @Query("""
            SELECT p1.dmChannel FROM DmChannelParticipant p1
            JOIN DmChannelParticipant p2 ON p1.dmChannel.id = p2.dmChannel.id
            WHERE p1.user.id = :userId1 AND p2.user.id = :userId2
            AND p1.dmChannel.type = :type
            AND p1.leftAt IS NULL AND p2.leftAt IS NULL
            """)
    Optional<DmChannel> findExistingChannel(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            @Param("type") DmChannelType type
    );
}

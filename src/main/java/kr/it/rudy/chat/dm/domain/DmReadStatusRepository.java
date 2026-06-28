package kr.it.rudy.chat.dm.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DmReadStatusRepository extends JpaRepository<DmReadStatus, Long> {
    Optional<DmReadStatus> findByDmChannelIdAndUserId(Long dmChannelId, Long userId);
}

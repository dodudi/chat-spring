package kr.it.rudy.chat.dm.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {
    List<DmMessage> findByDmChannelIdAndDeletedAtIsNullOrderByIdDesc(Long dmChannelId, Pageable pageable);
    List<DmMessage> findByDmChannelIdAndDeletedAtIsNullAndIdLessThanOrderByIdDesc(Long dmChannelId, Long before, Pageable pageable);
}

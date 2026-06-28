package kr.it.rudy.chat.dm.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DmMessageAttachmentRepository extends JpaRepository<DmMessageAttachment, Long> {
}

package kr.it.rudy.chat.dm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "dm_message_attachments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DmMessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dm_message_id", nullable = false)
    private DmMessage dmMessage;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String contentType;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private DmMessageAttachment(DmMessage dmMessage, String fileName, String fileUrl, Long fileSize, String contentType) {
        this.dmMessage = dmMessage;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public static DmMessageAttachment create(DmMessage dmMessage, String fileName, String fileUrl, Long fileSize, String contentType) {
        return DmMessageAttachment.builder()
                .dmMessage(dmMessage)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .contentType(contentType)
                .build();
    }
}

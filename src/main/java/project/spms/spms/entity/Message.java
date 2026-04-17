package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "from_id")
    private Integer fromId;
    @Column(name = "to_id", nullable = false)
    private Integer toId;
    @Column(length = 200)
    private String subject;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    @Column(name = "is_read")
    private Boolean isRead = false;
    // ── Attachment fields (Bug Fix #5: file attachments in messages) ──
    @Column(name = "attachment_filename", length = 255)
    private String attachmentFilename;
    @Column(name = "attachment_filepath", length = 500)
    private String attachmentFilepath;
    @Column(name = "attachment_filesize", length = 20)
    private String attachmentFilesize;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
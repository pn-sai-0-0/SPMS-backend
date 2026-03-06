package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private NotifType type = NotifType.info;
    @Column(name = "is_read")
    private Boolean isRead = false;
    @Column(length = 100)
    private String link = "overview";
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum NotifType {
        info, warning, review, success, error
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
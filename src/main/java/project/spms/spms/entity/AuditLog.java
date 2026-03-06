package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_id")
    private Integer userId;
    @Column(name = "user_name", length = 100)
    private String userName;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String action;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuditType type = AuditType.system;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AuditType {
        create, update, delete, assign, login, logout, upload, system, config, report, permission, revoke
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
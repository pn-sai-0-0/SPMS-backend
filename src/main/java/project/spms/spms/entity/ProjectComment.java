package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "project_comments")
public class ProjectComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "project_id", nullable = false)
    private Integer projectId;
    @Column(name = "author_id")
    private Integer authorId;
    @Column(name = "author_name", length = 100)
    private String authorName;
    @Column(name = "author_role", length = 50)
    private String authorRole;
    @Column(name = "comment_text", columnDefinition = "TEXT", nullable = false)
    private String commentText;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
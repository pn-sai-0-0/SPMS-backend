package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "project_assignments", uniqueConstraints = @UniqueConstraint(columnNames = { "project_id", "user_id" }))
public class ProjectAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "project_id", nullable = false)
    private Integer projectId;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(name = "role_in_project", length = 100)
    private String roleInProject = "Team Member";
    @Column(name = "assigned_by")
    private Integer assignedBy;
    private Integer commits = 0;
    @Column(name = "hours_contributed")
    private Integer hoursContributed = 0;
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}
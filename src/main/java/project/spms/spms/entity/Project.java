package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, nullable = false, length = 20)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "department_id")
    private Integer departmentId;
    @Column(name = "department_name", length = 100)
    private String departmentName;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProjectStatus status = ProjectStatus.unassigned;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority = Priority.medium;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Urgency urgency = Urgency.medium;
    private Integer progress = 0;
    @Column(name = "progress_note", length = 255)
    private String progressNote;
    @Column(name = "start_date")
    private LocalDate startDate;
    private LocalDate deadline;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ProjectStatus {
        unassigned, active, completed, delayed, archived
    }

    public enum Priority {
        low, medium, high, critical
    }

    public enum Urgency {
        low, medium, high
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
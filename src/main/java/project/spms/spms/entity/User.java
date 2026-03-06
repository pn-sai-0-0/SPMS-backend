package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "employee_id", unique = true, length = 20)
    private String employeeId;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(unique = true, length = 150)
    private String email;
    @Column(unique = true, length = 50)
    private String username;
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    @Column(name = "department_id")
    private Integer departmentId;
    @Column(name = "department_name", length = 100)
    private String departmentName;
    @Column(name = "manager_id")
    private Integer managerId;
    @Column(columnDefinition = "JSON")
    private String skills;
    private Integer workload = 0;
    @Column(name = "hours_per_week")
    private Integer hoursPerWeek = 40;
    @Column(name = "performance_score")
    private Integer performanceScore = 85;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Status status = Status.active;
    @Column(name = "avatar_initials", length = 5)
    private String avatarInitials;
    @Column(name = "join_date")
    private LocalDate joinDate;
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Transient
    private Integer projectCount;
    @Transient
    private Integer projectsCompleted;
    @Transient
    private Integer projectsOnTime;
    @Transient
    private Integer tasksCompleted;

    public enum Role {
        employee, manager, hr, admin
    }

    public enum Status {
        active, inactive
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
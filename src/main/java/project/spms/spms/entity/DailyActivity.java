package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "daily_activities", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "activity_date" }))
public class DailyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;
    @Column(name = "hours_worked", precision = 4, scale = 1)
    private BigDecimal hoursWorked = BigDecimal.ZERO;
    private Integer commits = 0;
    @Column(name = "tasks_done")
    private Integer tasksDone = 0;
    @Enumerated(EnumType.STRING)
    @Column(name = "stress_level", length = 10)
    private StressLevel stressLevel = StressLevel.low;

    public enum StressLevel {
        none, low, medium, high
    }
}
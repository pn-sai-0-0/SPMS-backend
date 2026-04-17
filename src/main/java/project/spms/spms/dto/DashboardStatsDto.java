package project.spms.spms.dto;

import lombok.Data;

@Data
public class DashboardStatsDto {
    private Integer activeProjects;
    private Integer tasksCompleted;
    private Integer pendingReviews;
    private Double hoursThisMonth;
    private Integer productivityScore;
    private Integer totalCommits;
    private Double avgHoursPerDay;
}
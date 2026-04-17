package project.spms.spms.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.test.context.ActiveProfiles;
import project.spms.spms.entity.*;
import project.spms.spms.repository.*;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  INTEGRATION TESTS — JPA Repositories (H2 In-Memory Database)      │
 * │                                                                     │
 * │  Strategy: @DataJpaTest spins up only the JPA slice of Spring.      │
 * │  H2 replaces MySQL — no network connection needed.                  │
 * │  Tests verify that all custom JPQL / native queries work correctly. │
 * │                                                                     │
 * │  Run: mvn test -Dtest=RepositoryIntegrationTest                     │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // use H2
@DisplayName("Integration Tests — JPA Repositories")
class RepositoryIntegrationTest {

    @Autowired private UserRepository userRepo;
    @Autowired private ProjectRepository projRepo;
    @Autowired private ProjectAssignmentRepository assignRepo;
    @Autowired private DailyActivityRepository activityRepo;
    @Autowired private DepartmentRepository deptRepo;
    @Autowired private NotificationRepository notifRepo;
    @Autowired private MessageRepository messageRepo;
    @Autowired private AuditLogRepository auditRepo;
    @Autowired private ProjectCommentRepository commentRepo;

    // ── Shared seed data created before each test ─────────────────────────────
    private User savedEmployee;
    private User savedManager;
    private Project savedProject;

    @BeforeEach
    void seedData() {
        // Employee
        User emp = new User();
        emp.setUsername("john.doe");
        emp.setName("John Doe");
        emp.setEmail("john@spms.com");
        emp.setPassword("$2a$10$hashed");
        emp.setRole(User.Role.employee);
        emp.setStatus(User.Status.active);
        emp.setDepartmentName("Engineering");
        emp.setEmployeeId("EMP-001");
        savedEmployee = userRepo.saveAndFlush(emp);

        // Manager
        User mgr = new User();
        mgr.setUsername("sarah.adams");
        mgr.setName("Sarah Adams");
        mgr.setEmail("sarah@spms.com");
        mgr.setPassword("$2a$10$hashed");
        mgr.setRole(User.Role.manager);
        mgr.setStatus(User.Status.active);
        mgr.setDepartmentName("Engineering");
        mgr.setEmployeeId("MGR-001");
        savedManager = userRepo.saveAndFlush(mgr);

        // Employee managed by manager
        savedEmployee.setManagerId(savedManager.getId());
        savedEmployee = userRepo.saveAndFlush(savedEmployee);

        // Project
        Project p = new Project();
        p.setCode("PROJ-001");
        p.setName("E-Commerce Redesign");
        p.setDepartmentName("Engineering");
        p.setStatus(Project.ProjectStatus.active);
        p.setPriority(Project.Priority.high);
        p.setUrgency(Project.Urgency.medium);
        p.setProgress(60);
        p.setDeadline(LocalDate.now().plusDays(30));
        savedProject = projRepo.saveAndFlush(p);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USER REPOSITORY TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UserRepository")
    class UserRepositoryTests {

        @Test
        @DisplayName("✅ findByUsernameAndRole returns correct user")
        void findByUsernameAndRole_match_returnsUser() {
            Optional<User> found = userRepo.findByUsernameAndRole("john.doe", User.Role.employee);

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("❌ findByUsernameAndRole returns empty for wrong role")
        void findByUsernameAndRole_wrongRole_returnsEmpty() {
            Optional<User> found = userRepo.findByUsernameAndRole("john.doe", User.Role.admin);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("✅ findByRole returns only employees")
        void findByRole_employee_returnsOnlyEmployees() {
            List<User> employees = userRepo.findByRole(User.Role.employee);

            assertThat(employees).isNotEmpty();
            assertThat(employees).allMatch(u -> u.getRole() == User.Role.employee);
        }

        @Test
        @DisplayName("✅ existsByUsername returns true for existing username")
        void existsByUsername_existing_returnsTrue() {
            assertThat(userRepo.existsByUsername("john.doe")).isTrue();
        }

        @Test
        @DisplayName("❌ existsByUsername returns false for unknown username")
        void existsByUsername_unknown_returnsFalse() {
            assertThat(userRepo.existsByUsername("nobody")).isFalse();
        }

        @Test
        @DisplayName("✅ existsByEmail returns true for existing email")
        void existsByEmail_existing_returnsTrue() {
            assertThat(userRepo.existsByEmail("john@spms.com")).isTrue();
        }

        @Test
        @DisplayName("✅ findByManagerIdAndRole filters by manager and role")
        void findByManagerIdAndRole_returnsTeam() {
            List<User> team = userRepo.findByManagerIdAndRole(savedManager.getId(), User.Role.employee);

            assertThat(team).hasSize(1);
            assertThat(team.get(0).getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("✅ findByStatus returns only active users")
        void findByStatus_active_returnsActiveUsers() {
            List<User> active = userRepo.findByStatus(User.Status.active);

            assertThat(active).isNotEmpty();
            assertThat(active).allMatch(u -> u.getStatus() == User.Status.active);
        }

        @Test
        @DisplayName("✅ updateLastLogin modifies the lastLogin field")
        void updateLastLogin_updatesCorrectly() {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            userRepo.updateLastLogin(savedEmployee.getId(), now);

            User refreshed = userRepo.findById(savedEmployee.getId()).orElseThrow();
            assertThat(refreshed.getLastLogin()).isNotNull();
        }

        @Test
        @DisplayName("✅ updateStatus changes user status to inactive")
        void updateStatus_toInactive_updatesCorrectly() {
            userRepo.updateStatus(savedEmployee.getId(), "inactive");

            User refreshed = userRepo.findById(savedEmployee.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo(User.Status.inactive);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROJECT REPOSITORY TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ProjectRepository")
    class ProjectRepositoryTests {

        @Test
        @DisplayName("✅ findByDepartmentName returns projects for that dept")
        void findByDepartmentName_engineering_returnsProject() {
            List<Project> projects = projRepo.findByDepartmentName("Engineering");

            assertThat(projects).hasSize(1);
            assertThat(projects.get(0).getCode()).isEqualTo("PROJ-001");
        }

        @Test
        @DisplayName("✅ existsByCode returns true for existing code")
        void existsByCode_existing_returnsTrue() {
            assertThat(projRepo.existsByCode("PROJ-001")).isTrue();
        }

        @Test
        @DisplayName("❌ existsByCode returns false for new code")
        void existsByCode_new_returnsFalse() {
            assertThat(projRepo.existsByCode("PROJ-999")).isFalse();
        }

        @Test
        @DisplayName("✅ updateProgress updates the progress field")
        void updateProgress_changesValue() {
            projRepo.updateProgress(savedProject.getId(), 80, "Almost done");

            Project refreshed = projRepo.findById(savedProject.getId()).orElseThrow();
            assertThat(refreshed.getProgress()).isEqualTo(80);
        }

        @Test
        @DisplayName("✅ findByUserId returns projects for assigned user")
        void findByUserId_assignedUser_returnsProject() {
            // Assign employee to project first
            ProjectAssignment a = new ProjectAssignment();
            a.setProjectId(savedProject.getId());
            a.setUserId(savedEmployee.getId());
            a.setRoleInProject("Developer");
            assignRepo.save(a);

            List<Project> userProjects = projRepo.findByUserId(savedEmployee.getId());

            assertThat(userProjects).hasSize(1);
            assertThat(userProjects.get(0).getCode()).isEqualTo("PROJ-001");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROJECT ASSIGNMENT REPOSITORY TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ProjectAssignmentRepository")
    class AssignmentRepositoryTests {

        @Test
        @DisplayName("✅ countByUserId returns correct assignment count")
        void countByUserId_returnsCount() {
            ProjectAssignment a = new ProjectAssignment();
            a.setProjectId(savedProject.getId());
            a.setUserId(savedEmployee.getId());
            a.setRoleInProject("Dev");
            assignRepo.save(a);

            Long count = assignRepo.countByUserId(savedEmployee.getId());

            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("✅ existsByProjectIdAndUserId detects existing assignment")
        void existsByProjectIdAndUserId_existing_returnsTrue() {
            ProjectAssignment a = new ProjectAssignment();
            a.setProjectId(savedProject.getId());
            a.setUserId(savedEmployee.getId());
            a.setRoleInProject("Dev");
            assignRepo.save(a);

            assertThat(assignRepo.existsByProjectIdAndUserId(savedProject.getId(), savedEmployee.getId()))
                    .isTrue();
        }

        @Test
        @DisplayName("✅ deleteAllByProjectId removes all project assignments")
        void deleteAllByProjectId_removesAll() {
            ProjectAssignment a = new ProjectAssignment();
            a.setProjectId(savedProject.getId());
            a.setUserId(savedEmployee.getId());
            a.setRoleInProject("Dev");
            assignRepo.save(a);

            assignRepo.deleteAllByProjectId(savedProject.getId());

            assertThat(assignRepo.findByProjectId(savedProject.getId())).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DAILY ACTIVITY REPOSITORY TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DailyActivityRepository")
    class DailyActivityRepositoryTests {

        @Test
        @DisplayName("✅ sumTasksInRange aggregates tasks in date range correctly")
        void sumTasksInRange_withinRange_returnsSum() {
            DailyActivity da = new DailyActivity();
            da.setUserId(savedEmployee.getId());
            da.setActivityDate(LocalDate.now());
            da.setTasksDone(7);
            da.setHoursWorked(8.0);
            da.setCommits(5);
            activityRepo.save(da);

            Integer sum = activityRepo.sumTasksInRange(
                    savedEmployee.getId(),
                    LocalDate.now().minusDays(1),
                    LocalDate.now()
            );

            assertThat(sum).isEqualTo(7);
        }

        @Test
        @DisplayName("✅ sumTasksInRange returns null when no data in range")
        void sumTasksInRange_noData_returnsNull() {
            Integer sum = activityRepo.sumTasksInRange(
                    savedEmployee.getId(),
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2020, 1, 31)
            );

            assertThat(sum).isNull();
        }

        @Test
        @DisplayName("✅ findByUserIdOrderByActivityDateDesc returns ordered list")
        void findByUserIdOrderByActivityDateDesc_returnsOrderedData() {
            for (int i = 0; i < 3; i++) {
                DailyActivity da = new DailyActivity();
                da.setUserId(savedEmployee.getId());
                da.setActivityDate(LocalDate.now().minusDays(i));
                da.setHoursWorked(8.0);
                da.setCommits(3);
                da.setTasksDone(2);
                activityRepo.save(da);
            }

            List<DailyActivity> activities =
                    activityRepo.findByUserIdOrderByActivityDateDesc(savedEmployee.getId());

            assertThat(activities).hasSize(3);
            // Verify descending order
            for (int i = 0; i < activities.size() - 1; i++) {
                assertThat(activities.get(i).getActivityDate())
                        .isAfterOrEqualTo(activities.get(i + 1).getActivityDate());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION REPOSITORY TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("NotificationRepository")
    class NotificationRepositoryTests {

        @Test
        @DisplayName("✅ findByUserIdOrderByCreatedAtDesc returns notifications for user")
        void findByUserId_returnsUserNotifications() {
            Notification n = new Notification();
            n.setUserId(savedEmployee.getId());
            n.setTitle("Test Notification");
            n.setMessage("You have a new message");
            n.setType(Notification.NotifType.info);
            n.setLink("overview");
            notifRepo.save(n);

            List<Notification> notifs =
                    notifRepo.findByUserIdOrderByCreatedAtDesc(savedEmployee.getId());

            assertThat(notifs).hasSize(1);
            assertThat(notifs.get(0).getTitle()).isEqualTo("Test Notification");
        }

        @Test
        @DisplayName("✅ countByUserIdAndIsReadFalse counts only unread notifications")
        void countUnread_returnsCorrectCount() {
            for (int i = 0; i < 3; i++) {
                Notification n = new Notification();
                n.setUserId(savedEmployee.getId());
                n.setTitle("Notif " + i);
                n.setMessage("message");
                n.setType(Notification.NotifType.info);
                n.setLink("overview");
                n.setIsRead(i == 0); // only first one is read
                notifRepo.save(n);
            }

            Long unread = notifRepo.countByUserIdAndIsReadFalse(savedEmployee.getId());

            assertThat(unread).isEqualTo(2L); // 2 unread
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AUDIT LOG REPOSITORY TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AuditLogRepository")
    class AuditLogRepositoryTests {

        @Test
        @DisplayName("✅ Saving audit log persists all fields correctly")
        void saveAuditLog_persistsAllFields() {
            AuditLog log = new AuditLog();
            log.setUserId(savedEmployee.getId());
            log.setUserName("John Doe");
            log.setAction("User logged in");
            log.setType(AuditLog.AuditType.login);
            AuditLog saved = auditRepo.save(log);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUserName()).isEqualTo("John Doe");
            assertThat(saved.getType()).isEqualTo(AuditLog.AuditType.login);
        }
    }
}

package project.spms.spms.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import project.spms.spms.entity.*;
import project.spms.spms.repository.*;
import project.spms.spms.service.*;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  SERVICE INTEGRATION TESTS                                          │
 * │                                                                     │
 * │  Strategy: @SpringBootTest loads the FULL application context but   │
 * │  application-test.properties swaps MySQL for H2 in-memory.         │
 * │  This verifies that Service + Repository + DB all work together     │
 * │  without mocks — the real wiring is tested here.                    │
 * │                                                                     │
 * │  @Transactional on each test rolls back changes after each test     │
 * │  so tests are fully isolated from one another.                      │
 * │                                                                     │
 * │  Run: mvn test -Dtest=ServiceIntegrationTest                        │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Service Integration Tests — Full Spring Context with H2")
class ServiceIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private NotificationService notifService;
    @Autowired private AuditService auditService;
    @Autowired private UserRepository userRepo;
    @Autowired private ProjectRepository projRepo;
    @Autowired private ProjectAssignmentRepository assignRepo;
    @Autowired private AuditLogRepository auditRepo;
    @Autowired private NotificationRepository notifRepo;
    @Autowired private BCryptPasswordEncoder encoder;

    // ── Test fixtures seeded directly into H2 ────────────────────────────────
    private User adminUser;
    private User employeeUser;
    private User managerUser;

    @BeforeEach
    void seedTestData() {
        // Create admin
        User admin = new User();
        admin.setUsername("test_admin");
        admin.setName("Test Admin");
        admin.setEmail("admin@test.spms.com");
        admin.setPassword(encoder.encode("password"));
        admin.setRole(User.Role.admin);
        admin.setStatus(User.Status.active);
        admin.setEmployeeId("ADM-TEST-001");
        admin.setAvatarInitials("TA");
        admin.setJoinDate(LocalDate.now());
        adminUser = userRepo.save(admin);

        // Create manager
        User mgr = new User();
        mgr.setUsername("test_manager");
        mgr.setName("Test Manager");
        mgr.setEmail("manager@test.spms.com");
        mgr.setPassword(encoder.encode("password"));
        mgr.setRole(User.Role.manager);
        mgr.setStatus(User.Status.active);
        mgr.setEmployeeId("MGR-TEST-001");
        mgr.setAvatarInitials("TM");
        mgr.setJoinDate(LocalDate.now());
        mgr.setDepartmentName("Engineering");
        managerUser = userRepo.save(mgr);

        // Create employee (reports to manager)
        User emp = new User();
        emp.setUsername("test_employee");
        emp.setName("Test Employee");
        emp.setEmail("employee@test.spms.com");
        emp.setPassword(encoder.encode("password"));
        emp.setRole(User.Role.employee);
        emp.setStatus(User.Status.active);
        emp.setEmployeeId("EMP-TEST-001");
        emp.setAvatarInitials("TE");
        emp.setJoinDate(LocalDate.now());
        emp.setDepartmentName("Engineering");
        emp.setManagerId(managerUser.getId());
        employeeUser = userRepo.save(emp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USER SERVICE INTEGRATION TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UserService — integrated with real DB")
    class UserServiceIntegrationTests {

        @Test
        @DisplayName("✅ login() with correct credentials should succeed end-to-end")
        void login_correctCredentials_succeeds() {
            Optional<User> result = userService.login("test_employee", "employee", "password");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Test Employee");
            assertThat(result.get().getLastLogin()).isNotNull(); // updateLastLogin was called
        }

        @Test
        @DisplayName("❌ login() with wrong password should return empty")
        void login_wrongPassword_returnsEmpty() {
            Optional<User> result = userService.login("test_employee", "employee", "wrongpass");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("✅ login() should write an audit log entry for successful login")
        void login_success_createsAuditLog() {
            long auditCountBefore = auditRepo.count();

            userService.login("test_employee", "employee", "password");

            long auditCountAfter = auditRepo.count();
            assertThat(auditCountAfter).isGreaterThan(auditCountBefore);
        }

        @Test
        @DisplayName("✅ create() should persist user with BCrypt-encoded password")
        void create_newUser_passwordIsEncoded() {
            User newUser = new User();
            newUser.setUsername("brand_new");
            newUser.setName("Brand New");
            newUser.setEmail("brand.new@test.spms.com");
            newUser.setRole(User.Role.employee);
            newUser.setDepartmentName("Engineering");

            User saved = userService.create(newUser, "mysecret", adminUser.getId(), adminUser.getName());

            // Verify password is encoded, not stored in plain text
            assertThat(saved.getPassword()).doesNotContain("mysecret");
            assertThat(encoder.matches("mysecret", saved.getPassword())).isTrue();
        }

        @Test
        @DisplayName("✅ create() should auto-generate employeeId based on role count")
        void create_employee_autoGeneratesEmployeeId() {
            User newUser = new User();
            newUser.setUsername("emp_auto");
            newUser.setName("Auto Employee");
            newUser.setEmail("emp.auto@test.spms.com");
            newUser.setRole(User.Role.employee);

            User saved = userService.create(newUser, "pass", adminUser.getId(), adminUser.getName());

            assertThat(saved.getEmployeeId()).startsWith("EMP-");
            assertThat(saved.getEmployeeId()).matches("EMP-\\d{3}");
        }

        @Test
        @DisplayName("✅ create() should set avatar initials from first letters of name parts")
        void create_fullName_setsCorrectInitials() {
            User newUser = new User();
            newUser.setUsername("john_smith");
            newUser.setName("John Smith");
            newUser.setEmail("john.smith@test.spms.com");
            newUser.setRole(User.Role.employee);

            User saved = userService.create(newUser, "pass", adminUser.getId(), adminUser.getName());

            assertThat(saved.getAvatarInitials()).isEqualTo("JS");
        }

        @Test
        @DisplayName("❌ create() should throw if username is already taken")
        void create_duplicateUsername_throwsException() {
            User duplicate = new User();
            duplicate.setUsername("test_employee"); // already exists
            duplicate.setName("Duplicate");
            duplicate.setEmail("dup@test.spms.com");
            duplicate.setRole(User.Role.employee);

            assertThatThrownBy(() ->
                    userService.create(duplicate, "pass", adminUser.getId(), adminUser.getName()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already taken");
        }

        @Test
        @DisplayName("✅ changePassword() should update password and be verifiable on next login")
        void changePassword_thenLogin_succeedsWithNewPassword() {
            // Change password
            userService.changePassword(
                    employeeUser.getId(), "newpass999",
                    adminUser.getId(), adminUser.getName()
            );

            // Old password should no longer work
            Optional<User> oldLogin = userService.login("test_employee", "employee", "password");
            assertThat(oldLogin).isEmpty();

            // New password should work
            Optional<User> newLogin = userService.login("test_employee", "employee", "newpass999");
            assertThat(newLogin).isPresent();
        }

        @Test
        @DisplayName("✅ updateStatus() to inactive should block login")
        void updateStatus_inactive_blocksLogin() {
            userService.updateStatus(employeeUser.getId(), "inactive",
                    adminUser.getId(), adminUser.getName());

            Optional<User> result = userService.login("test_employee", "employee", "password");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("✅ delete() should remove user from repository")
        void delete_existingUser_removesFromDB() {
            Integer idToDelete = employeeUser.getId();
            assertThat(userRepo.findById(idToDelete)).isPresent();

            userService.delete(idToDelete, adminUser.getId(), adminUser.getName());

            assertThat(userRepo.findById(idToDelete)).isEmpty();
        }

        @Test
        @DisplayName("✅ getByManagerId() should return only employees under given manager")
        void getByManagerId_returnsCorrectTeam() {
            List<User> team = userService.getByManagerId(managerUser.getId());

            assertThat(team).hasSize(1);
            assertThat(team.get(0).getName()).isEqualTo("Test Employee");
            assertThat(team.get(0).getRole()).isEqualTo(User.Role.employee);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROJECT SERVICE INTEGRATION TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ProjectService — integrated with real DB")
    class ProjectServiceIntegrationTests {

        @Test
        @DisplayName("✅ create() should persist project with correct code and audit log")
        void create_newProject_persistsAndLogs() {
            long auditBefore = auditRepo.count();

            Map<String, Object> body = Map.of(
                    "name", "Integration Test Project",
                    "departmentName", "Engineering",
                    "priority", "high",
                    "urgency", "medium",
                    "deadline", LocalDate.now().plusDays(60).toString()
            );

            Project created = projectService.create(body, managerUser.getId(), managerUser.getName());

            assertThat(created.getId()).isNotNull();
            assertThat(created.getCode()).matches("PROJ-\\d{3}");
            assertThat(created.getName()).isEqualTo("Integration Test Project");
            assertThat(created.getStatus()).isEqualTo(Project.ProjectStatus.unassigned);
            assertThat(auditRepo.count()).isGreaterThan(auditBefore);
        }

        @Test
        @DisplayName("✅ assign() should activate unassigned project and send notification")
        void assign_toUnassignedProject_activatesAndNotifies() {
            // Create a project (will be unassigned)
            Map<String, Object> body = Map.of("name", "Assign Test", "departmentName", "Engineering");
            Project project = projectService.create(body, managerUser.getId(), managerUser.getName());
            assertThat(project.getStatus()).isEqualTo(Project.ProjectStatus.unassigned);

            long notifBefore = notifRepo.count();

            // Assign the employee
            projectService.assign(
                    project.getId(),
                    List.of(employeeUser.getId()),
                    "Developer",
                    managerUser.getId(),
                    managerUser.getName()
            );

            // Project should now be active
            Project updated = projRepo.findById(project.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(Project.ProjectStatus.active);

            // Employee should have received a notification
            assertThat(notifRepo.count()).isGreaterThan(notifBefore);
        }

        @Test
        @DisplayName("✅ updateProgress() to 100 should auto-set status to completed")
        void updateProgress_100_autoCompletes() {
            Map<String, Object> body = Map.of("name", "Progress Test", "departmentName", "HR");
            Project project = projectService.create(body, managerUser.getId(), managerUser.getName());

            projectService.updateProgress(
                    project.getId(), 100, "All done!",
                    employeeUser.getId(), employeeUser.getName()
            );

            Project updated = projRepo.findById(project.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(Project.ProjectStatus.completed);
            assertThat(updated.getProgress()).isEqualTo(100);
        }

        @Test
        @DisplayName("✅ delete() should remove project and all assignments")
        void delete_projectWithAssignments_removesAll() {
            // Create project and assign
            Map<String, Object> body = Map.of("name", "Delete Me", "departmentName", "HR");
            Project project = projectService.create(body, managerUser.getId(), managerUser.getName());
            projectService.assign(project.getId(), List.of(employeeUser.getId()),
                    "Dev", managerUser.getId(), managerUser.getName());

            assertThat(assignRepo.findByProjectId(project.getId())).isNotEmpty();

            // Delete project
            projectService.delete(project.getId(), adminUser.getId(), adminUser.getName());

            // Both project and assignments should be gone
            assertThat(projRepo.findById(project.getId())).isEmpty();
            assertThat(assignRepo.findByProjectId(project.getId())).isEmpty();
        }

        @Test
        @DisplayName("✅ getDetail() should return fully populated DTO after assignment")
        void getDetail_afterAssignment_returnsTeamMembers() {
            Map<String, Object> body = Map.of("name", "Detail Test", "departmentName", "Engineering");
            Project project = projectService.create(body, managerUser.getId(), managerUser.getName());
            projectService.assign(project.getId(), List.of(employeeUser.getId()),
                    "Tester", managerUser.getId(), managerUser.getName());

            var detail = projectService.getDetail(project.getId());

            assertThat(detail.getTeam()).hasSize(1);
            assertThat(detail.getTeam().get(0).getName()).isEqualTo("Test Employee");
            assertThat(detail.getTeam().get(0).getRoleInProject()).isEqualTo("Tester");
        }

        @Test
        @DisplayName("✅ assign() duplicate — should NOT create duplicate assignment")
        void assign_twice_onlyOneAssignmentCreated() {
            Map<String, Object> body = Map.of("name", "Dup Test", "departmentName", "Engineering");
            Project project = projectService.create(body, managerUser.getId(), managerUser.getName());

            projectService.assign(project.getId(), List.of(employeeUser.getId()),
                    "Dev", managerUser.getId(), managerUser.getName());
            projectService.assign(project.getId(), List.of(employeeUser.getId()),
                    "Dev", managerUser.getId(), managerUser.getName());

            long assignments = assignRepo.findByProjectId(project.getId()).size();
            assertThat(assignments).isEqualTo(1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION SERVICE INTEGRATION TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("NotificationService — integrated with real DB")
    class NotificationServiceIntegrationTests {

        @Test
        @DisplayName("✅ sendInfo() should persist notification to DB")
        void sendInfo_persists() {
            Notification saved = notifService.sendInfo(
                    employeeUser.getId(), "Welcome", "You have been added to SPMS", "overview");

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getType()).isEqualTo(Notification.NotifType.info);

            Optional<Notification> fromDB = notifRepo.findById(saved.getId());
            assertThat(fromDB).isPresent();
            assertThat(fromDB.get().getTitle()).isEqualTo("Welcome");
        }

        @Test
        @DisplayName("✅ sendWarning() should persist notification with warning type")
        void sendWarning_persistsCorrectType() {
            Notification saved = notifService.sendWarning(
                    employeeUser.getId(), "Deadline Soon", "Your project deadline is in 2 days", "projects");

            assertThat(notifRepo.findById(saved.getId()))
                    .isPresent()
                    .hasValueSatisfying(n -> assertThat(n.getType()).isEqualTo(Notification.NotifType.warning));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AUDIT SERVICE INTEGRATION TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AuditService — integrated with real DB")
    class AuditServiceIntegrationTests {

        @Test
        @DisplayName("✅ log() should persist audit entry with all fields")
        void log_persistsAllFields() {
            auditService.log(adminUser.getId(), adminUser.getName(),
                    "Test audit action", AuditLog.AuditType.update);

            List<AuditLog> logs = auditRepo.findAll();
            Optional<AuditLog> testLog = logs.stream()
                    .filter(l -> "Test audit action".equals(l.getAction()))
                    .findFirst();

            assertThat(testLog).isPresent();
            assertThat(testLog.get().getUserName()).isEqualTo(adminUser.getName());
            assertThat(testLog.get().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("✅ All audit operations produce audit trail entries")
        void userLifecycle_producesAuditTrail() {
            long before = auditRepo.count();

            // Create → login → change password → delete each produce audit entries
            User u = new User();
            u.setUsername("audit_trail_user");
            u.setName("Audit Trail");
            u.setEmail("audit_trail@test.spms.com");
            u.setRole(User.Role.employee);
            User created = userService.create(u, "pass", adminUser.getId(), adminUser.getName());
            userService.login("audit_trail_user", "employee", "pass");
            userService.changePassword(created.getId(), "newpass", adminUser.getId(), adminUser.getName());
            userService.delete(created.getId(), adminUser.getId(), adminUser.getName());

            // Should have at least 4 audit entries (create, login, change password, delete)
            assertThat(auditRepo.count()).isGreaterThanOrEqualTo(before + 4);
        }
    }
}

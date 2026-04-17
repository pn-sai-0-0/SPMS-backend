package project.spms.spms;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import project.spms.spms.controller.ApiController;
import project.spms.spms.repository.*;
import project.spms.spms.service.*;

import static org.assertj.core.api.Assertions.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  SMOKE TEST — Spring Application Context Loads                     │
 * │                                                                     │
 * │  This is the first test that should pass. It verifies that the     │
 * │  entire Spring context boots without errors using the test profile  │
 * │  (H2 in-memory database instead of MySQL).                         │
 * │                                                                     │
 * │  If this fails, something is fundamentally broken in the app       │
 * │  configuration and all other tests will also fail.                 │
 * │                                                                     │
 * │  Run: mvn test -Dtest=SpmsApplicationTests                         │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Smoke Tests — Application Context")
class SpmsApplicationTests {

    @Autowired private ApplicationContext context;

    // ── Services ──────────────────────────────────────────────────────────────
    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditService auditService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    // ── Repositories ──────────────────────────────────────────────────────────
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProjectAssignmentRepository assignmentRepository;
    @Autowired private ProjectFileRepository fileRepository;
    @Autowired private ProjectCommentRepository commentRepository;
    @Autowired private ProjectHistoryRepository historyRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private DailyActivityRepository dailyActivityRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private AppSettingRepository appSettingRepository;
    @Autowired private CompanyRepository companyRepository;

    // ── Controller ────────────────────────────────────────────────────────────
    @Autowired private ApiController apiController;

    // ══════════════════════════════════════════════════════════════════════════
    // CONTEXT LOADS
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Spring Application Context loads without errors")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("✅ Application context contains all expected bean classes")
    void context_containsAllExpectedBeans() {
        assertThat(context.getBeanNamesForType(UserService.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(ProjectService.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(NotificationService.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(AuditService.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(ApiController.class)).isNotEmpty();
        assertThat(context.getBeanNamesForType(BCryptPasswordEncoder.class)).isNotEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BEANS WIRED CORRECTLY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Bean Wiring — all @Autowired fields are non-null")
    class BeanWiringTests {

        @Test
        @DisplayName("✅ All service beans are injected and non-null")
        void serviceBeans_areNonNull() {
            assertThat(userService).isNotNull();
            assertThat(projectService).isNotNull();
            assertThat(notificationService).isNotNull();
            assertThat(auditService).isNotNull();
            assertThat(fileStorageService).isNotNull();
            assertThat(passwordEncoder).isNotNull();
        }

        @Test
        @DisplayName("✅ All repository beans are injected and non-null")
        void repositoryBeans_areNonNull() {
            assertThat(userRepository).isNotNull();
            assertThat(projectRepository).isNotNull();
            assertThat(departmentRepository).isNotNull();
            assertThat(assignmentRepository).isNotNull();
            assertThat(fileRepository).isNotNull();
            assertThat(commentRepository).isNotNull();
            assertThat(historyRepository).isNotNull();
            assertThat(notificationRepository).isNotNull();
            assertThat(auditLogRepository).isNotNull();
            assertThat(dailyActivityRepository).isNotNull();
            assertThat(messageRepository).isNotNull();
            assertThat(appSettingRepository).isNotNull();
            assertThat(companyRepository).isNotNull();
        }

        @Test
        @DisplayName("✅ ApiController bean is injected")
        void apiControllerBean_isNonNull() {
            assertThat(apiController).isNotNull();
        }

        @Test
        @DisplayName("✅ BCryptPasswordEncoder bean is properly configured")
        void passwordEncoder_isConfigured() {
            assertThat(passwordEncoder).isNotNull();
            // Quick sanity check: it can encode and verify
            String hash = passwordEncoder.encode("testpassword");
            assertThat(passwordEncoder.matches("testpassword", hash)).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATABASE CONNECTIVITY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Database — H2 in-memory DB connectivity")
    class DatabaseTests {

        @Test
        @DisplayName("✅ Can query users table without exception")
        void usersTable_queryable() {
            assertThatCode(() -> userRepository.count()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("✅ Can query projects table without exception")
        void projectsTable_queryable() {
            assertThatCode(() -> projectRepository.count()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("✅ Can query notifications table without exception")
        void notificationsTable_queryable() {
            assertThatCode(() -> notificationRepository.count()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("✅ Can query audit_log table without exception")
        void auditLogTable_queryable() {
            assertThatCode(() -> auditLogRepository.count()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("✅ H2 schema matches all entity classes (DDL validation)")
        void schema_matchesAllEntities() {
            // These will throw if any entity field doesn't map to a valid column
            assertThatCode(() -> {
                userRepository.findAll();
                projectRepository.findAll();
                assignmentRepository.findAll();
                notificationRepository.findAll();
                auditLogRepository.findAll();
                messageRepository.findAll();
                departmentRepository.findAll();
                dailyActivityRepository.findAll();
                commentRepository.findAll();
                historyRepository.findAll();
            }).doesNotThrowAnyException();
        }
    }
}

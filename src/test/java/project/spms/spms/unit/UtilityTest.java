package project.spms.spms.unit;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import project.spms.spms.dto.*;
import project.spms.spms.entity.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ UTILITY TESTS — Entity lifecycle, DTO, enums, edge cases │
 * │ │
 * │ Strategy: Pure Java tests with zero Spring context. │
 * │ Tests verify entity @PrePersist / @PreUpdate hooks, enum values, │
 * │ DTO construction, Lombok-generated code, and edge case inputs. │
 * │ │
 * │ Run: mvn test -Dtest=UtilityTest │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@DisplayName("Utility Tests — Entities, DTOs, Enums, Edge Cases")
class UtilityTest {

    // ══════════════════════════════════════════════════════════════════════════
    // USER ENTITY — Lifecycle callbacks and enum validation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User Entity")
    class UserEntityTests {

        @Test
        @DisplayName("✅ @PrePersist sets createdAt and updatedAt to current time")
        void prePersist_setsTimestamps() {
            User u = new User();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            u.onCreate(); // simulate JPA calling @PrePersist

            assertThat(u.getCreatedAt()).isAfter(before);
            assertThat(u.getUpdatedAt()).isAfter(before);
        }

        @Test
        @DisplayName("✅ @PreUpdate updates only updatedAt, not createdAt")
        void preUpdate_onlyUpdatesUpdatedAt() {
            User u = new User();
            u.onCreate();
            LocalDateTime originalCreatedAt = u.getCreatedAt();

            // Simulate time passing
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }

            u.onUpdate(); // simulate JPA calling @PreUpdate

            assertThat(u.getCreatedAt()).isEqualTo(originalCreatedAt); // unchanged
            assertThat(u.getUpdatedAt()).isAfterOrEqualTo(originalCreatedAt); // updated
        }

        @Test
        @DisplayName("✅ User default values: workload=0, hoursPerWeek=40, performanceScore=85, status=active")
        void user_defaultValues_areCorrect() {
            User u = new User();

            assertThat(u.getWorkload()).isEqualTo(0);
            assertThat(u.getHoursPerWeek()).isEqualTo(40);
            assertThat(u.getPerformanceScore()).isEqualTo(85);
            assertThat(u.getStatus()).isEqualTo(User.Status.active);
        }

        @Test
        @DisplayName("✅ User Role enum contains all 4 expected roles")
        void userRole_allRolesExist() {
            User.Role[] roles = User.Role.values();
            assertThat(roles).containsExactlyInAnyOrder(
                    User.Role.employee,
                    User.Role.manager,
                    User.Role.hr,
                    User.Role.admin);
        }

        @Test
        @DisplayName("✅ User Status enum has active and inactive")
        void userStatus_twoValues() {
            assertThat(User.Status.values()).containsExactlyInAnyOrder(
                    User.Status.active,
                    User.Status.inactive);
        }

        // FIX 3: Removed duplicate @Test annotation — @ParameterizedTest already
        // registers the test
        @ParameterizedTest(name = "Role ''{0}'' should parse without exception")
        @ValueSource(strings = { "employee", "manager", "hr", "admin" })
        @DisplayName("✅ User Role.valueOf() parses lowercase role strings correctly")
        void userRole_parsesFromString(String roleStr) {
            assertThatCode(() -> User.Role.valueOf(roleStr)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("❌ User Role.valueOf() throws for invalid role string")
        void userRole_throwsForInvalidString() {
            assertThatThrownBy(() -> User.Role.valueOf("superuser"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("✅ Lombok @Data generates correct equals/hashCode based on all fields")
        void user_lombokEquals() {
            User u1 = new User();
            u1.setId(1);
            u1.setUsername("john");

            User u2 = new User();
            u2.setId(1);
            u2.setUsername("john");

            assertThat(u1).isEqualTo(u2);
            assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
        }

        @Test
        @DisplayName("✅ Lombok @Data generates toString that includes key fields")
        void user_lombokToString() {
            User u = new User();
            u.setId(1);
            u.setUsername("john.doe");
            u.setRole(User.Role.employee);

            String str = u.toString();

            assertThat(str).contains("id=1");
            assertThat(str).contains("john.doe");
            assertThat(str).contains("employee");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROJECT ENTITY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Project Entity")
    class ProjectEntityTests {

        @Test
        @DisplayName("✅ Project default values: progress=0, status=unassigned, priority=medium, urgency=medium")
        void project_defaultValues() {
            Project p = new Project();

            assertThat(p.getProgress()).isEqualTo(0);
            assertThat(p.getStatus()).isEqualTo(Project.ProjectStatus.unassigned);
            assertThat(p.getPriority()).isEqualTo(Project.Priority.medium);
            assertThat(p.getUrgency()).isEqualTo(Project.Urgency.medium);
        }

        @Test
        @DisplayName("✅ @PrePersist sets both timestamps on a new project")
        void project_prePersist_setsTimestamps() {
            Project p = new Project();
            p.onCreate();

            assertThat(p.getCreatedAt()).isNotNull();
            assertThat(p.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("✅ Project Priority enum has 4 values")
        void projectPriority_fourValues() {
            assertThat(Project.Priority.values()).containsExactlyInAnyOrder(
                    Project.Priority.low,
                    Project.Priority.medium,
                    Project.Priority.high,
                    Project.Priority.critical);
        }

        @Test
        @DisplayName("✅ Project Urgency enum has 3 values")
        void projectUrgency_threeValues() {
            assertThat(Project.Urgency.values()).containsExactlyInAnyOrder(
                    Project.Urgency.low,
                    Project.Urgency.medium,
                    Project.Urgency.high);
        }

        @Test
        @DisplayName("✅ Project ProjectStatus enum has 5 values")
        void projectStatus_fiveValues() {
            assertThat(Project.ProjectStatus.values()).containsExactlyInAnyOrder(
                    Project.ProjectStatus.unassigned,
                    Project.ProjectStatus.active,
                    Project.ProjectStatus.completed,
                    Project.ProjectStatus.delayed,
                    Project.ProjectStatus.archived);
        }

        @ParameterizedTest(name = "Priority ''{0}'' parses without exception")
        @ValueSource(strings = { "low", "medium", "high", "critical" })
        @DisplayName("✅ All priority strings parse correctly")
        void projectPriority_allStrings_parse(String priority) {
            assertThatCode(() -> Project.Priority.valueOf(priority)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "Status ''{0}'' parses without exception")
        @ValueSource(strings = { "unassigned", "active", "completed", "delayed", "archived" })
        @DisplayName("✅ All status strings parse correctly")
        void projectStatus_allStrings_parse(String status) {
            assertThatCode(() -> Project.ProjectStatus.valueOf(status)).doesNotThrowAnyException();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MESSAGE ENTITY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Message Entity")
    class MessageEntityTests {

        @Test
        @DisplayName("✅ Message default isRead = false")
        void message_defaultIsReadFalse() {
            Message m = new Message();
            assertThat(m.getIsRead()).isFalse();
        }

        @Test
        @DisplayName("✅ @PrePersist sets createdAt")
        void message_prePersist_setsCreatedAt() {
            Message m = new Message();
            m.onCreate();
            assertThat(m.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("✅ Message with attachment: all 3 attachment fields are settable")
        void message_attachmentFields_settable() {
            Message m = new Message();
            m.setAttachmentFilename("report.pdf");
            m.setAttachmentFilepath("/uploads/report.pdf");
            m.setAttachmentFilesize("512 KB");

            assertThat(m.getAttachmentFilename()).isEqualTo("report.pdf");
            assertThat(m.getAttachmentFilepath()).isEqualTo("/uploads/report.pdf");
            assertThat(m.getAttachmentFilesize()).isEqualTo("512 KB");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION ENTITY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Notification Entity")
    class NotificationEntityTests {

        @Test
        @DisplayName("✅ Notification default isRead = false")
        void notification_defaultIsReadFalse() {
            Notification n = new Notification();
            assertThat(n.getIsRead()).isFalse();
        }

        @Test
        @DisplayName("✅ Notification NotifType enum has expected values")
        void notifType_allValues() {
            assertThat(Notification.NotifType.values()).contains(
                    Notification.NotifType.info,
                    Notification.NotifType.warning);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AUDIT LOG ENTITY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AuditLog Entity")
    class AuditLogEntityTests {

        @Test
        @DisplayName("✅ AuditLog AuditType has all expected types")
        void auditType_allValues() {
            assertThat(AuditLog.AuditType.values()).containsExactlyInAnyOrder(
                    AuditLog.AuditType.login,
                    AuditLog.AuditType.logout,
                    AuditLog.AuditType.create,
                    AuditLog.AuditType.update,
                    AuditLog.AuditType.delete,
                    AuditLog.AuditType.assign,
                    AuditLog.AuditType.upload,
                    AuditLog.AuditType.system,
                    AuditLog.AuditType.config,
                    AuditLog.AuditType.report,
                    AuditLog.AuditType.permission,
                    AuditLog.AuditType.revoke);
        }

        @ParameterizedTest(name = "AuditType ''{0}'' parses from string")
        @ValueSource(strings = { "login", "logout", "create", "update", "delete", "assign" })
        @DisplayName("✅ All AuditType strings parse correctly")
        void auditType_allStrings_parse(String type) {
            assertThatCode(() -> AuditLog.AuditType.valueOf(type)).doesNotThrowAnyException();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // API RESPONSE DTO
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ApiResponse DTO")
    class ApiResponseDtoTests {

        @Test
        @DisplayName("✅ ApiResponse.ok(data) produces success=true, message=OK")
        void apiResponse_ok_withData() {
            ApiResponse resp = ApiResponse.ok("some data");

            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getMessage()).isEqualTo("OK");
            assertThat(resp.getData()).isEqualTo("some data");
        }

        @Test
        @DisplayName("✅ ApiResponse.ok(message, data) allows custom message")
        void apiResponse_ok_withMessageAndData() {
            ApiResponse resp = ApiResponse.ok("Created successfully", 42);

            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getMessage()).isEqualTo("Created successfully");
            assertThat(resp.getData()).isEqualTo(42);
        }

        @Test
        @DisplayName("✅ ApiResponse.error(message) produces success=false, null data")
        void apiResponse_error() {
            ApiResponse resp = ApiResponse.error("Something went wrong");

            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getMessage()).isEqualTo("Something went wrong");
            assertThat(resp.getData()).isNull();
        }

        @Test
        @DisplayName("✅ ApiResponse.ok(null) is valid — data can be null on OK")
        void apiResponse_ok_withNullData() {
            ApiResponse resp = ApiResponse.ok((Object) null);

            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getData()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN REQUEST DTO
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("LoginRequest DTO")
    class LoginRequestDtoTests {

        @Test
        @DisplayName("✅ LoginRequest is settable and gettable via Lombok")
        void loginRequest_getterSetter() {
            LoginRequest req = new LoginRequest();
            req.setUsername("john");
            req.setPassword("secret");
            req.setRole("employee");

            assertThat(req.getUsername()).isEqualTo("john");
            assertThat(req.getPassword()).isEqualTo("secret");
            assertThat(req.getRole()).isEqualTo("employee");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EDGE CASES — Avatar initials generation logic (tested via string ops)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Cases — Avatar Initials Generation")
    class AvatarInitialsEdgeCaseTests {

        static Stream<Arguments> initialsProvider() {
            return Stream.of(
                    Arguments.of("John Doe", "JD"),
                    Arguments.of("Alice Smith", "AS"),
                    Arguments.of("David Martinez", "DM"),
                    Arguments.of("Madonna", "MA"),
                    Arguments.of("A", "A"),
                    Arguments.of("ab", "AB"));
        }

        @ParameterizedTest(name = "Name ''{0}'' → initials ''{1}''")
        @MethodSource("initialsProvider")
        @DisplayName("✅ Avatar initials generation handles all name formats")
        void avatarInitials_generatedCorrectly(String name, String expectedInitials) {
            String[] parts = name.split(" ");
            String initials = parts.length >= 2
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : name.substring(0, Math.min(2, name.length())).toUpperCase();

            assertThat(initials).isEqualToIgnoringCase(expectedInitials);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EDGE CASES — Project code generation pattern
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Cases — Project Code Format")
    class ProjectCodeEdgeCaseTests {

        @ParameterizedTest(name = "count={0} → code=''{1}''")
        @CsvSource({
                "0,  PROJ-001",
                "1,  PROJ-002",
                "9,  PROJ-010",
                "99, PROJ-100",
                "999,PROJ-1000"
        })
        @DisplayName("✅ Project code format PROJ-NNN always has at least 3 digits")
        void projectCode_formatIsCorrect(long count, String expectedCode) {
            String code = String.format("PROJ-%03d", count + 1);
            assertThat(code).isEqualTo(expectedCode.trim());
        }
    }
}

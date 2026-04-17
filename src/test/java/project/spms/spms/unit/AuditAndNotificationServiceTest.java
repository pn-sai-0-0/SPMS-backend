package project.spms.spms.unit;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import project.spms.spms.entity.*;
import project.spms.spms.repository.*;
import project.spms.spms.service.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  UNIT TESTS — AuditService & NotificationService                   │
 * │  Run: mvn test -Dtest=AuditAndNotificationServiceTest              │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests — AuditService & NotificationService")
class AuditAndNotificationServiceTest {

    // ── AuditService under test ───────────────────────────────────────────────
    @Mock private AuditLogRepository auditRepo;
    @InjectMocks private AuditService auditService;

    // ── NotificationService under test ────────────────────────────────────────
    @Mock private NotificationRepository notifRepo;
    @InjectMocks private NotificationService notifService;

    // ══════════════════════════════════════════════════════════════════════════
    // AUDIT SERVICE TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AuditService")
    class AuditServiceTests {

        @Test
        @DisplayName("✅ log(userId, userName, action, type) should persist to DB")
        void log_withUserId_savesEntry() {
            given(auditRepo.save(any(AuditLog.class))).willAnswer(inv -> inv.getArgument(0));

            auditService.log(1, "John Doe", "User logged in", AuditLog.AuditType.login);

            verify(auditRepo).save(argThat(log ->
                log.getUserId().equals(1) &&
                log.getUserName().equals("John Doe") &&
                log.getAction().equals("User logged in") &&
                log.getType() == AuditLog.AuditType.login
            ));
        }

        @Test
        @DisplayName("✅ log(userName, action, type) — userId should be null")
        void log_withoutUserId_savesWithNullId() {
            given(auditRepo.save(any(AuditLog.class))).willAnswer(inv -> inv.getArgument(0));

            auditService.log("System", "Scheduled job ran", AuditLog.AuditType.update);

            verify(auditRepo).save(argThat(log ->
                log.getUserId() == null &&
                log.getUserName().equals("System")
            ));
        }

        @Test
        @DisplayName("✅ All AuditType values can be logged without exception")
        void log_allAuditTypes_noException() {
            given(auditRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            for (AuditLog.AuditType type : AuditLog.AuditType.values()) {
                assertThatCode(() -> auditService.log(1, "User", "action", type))
                        .doesNotThrowAnyException();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION SERVICE TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("NotificationService")
    class NotificationServiceTests {

        @Test
        @DisplayName("✅ sendInfo() should create notification with INFO type")
        void sendInfo_createsInfoNotification() {
            Notification saved = new Notification();
            saved.setId(1);
            saved.setType(Notification.NotifType.info);
            given(notifRepo.save(any())).willReturn(saved);

            Notification n = notifService.sendInfo(5, "Project Assigned", "You joined PROJ-001", "projects");

            assertThat(n.getType()).isEqualTo(Notification.NotifType.info);
            verify(notifRepo).save(argThat(notif ->
                notif.getUserId().equals(5) &&
                notif.getTitle().equals("Project Assigned") &&
                notif.getLink().equals("projects") &&
                notif.getType() == Notification.NotifType.info
            ));
        }

        @Test
        @DisplayName("✅ sendWarning() should create notification with WARNING type")
        void sendWarning_createsWarningNotification() {
            given(notifRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            notifService.sendWarning(5, "Deadline Soon", "Project due in 2 days", "projects");

            verify(notifRepo).save(argThat(n -> n.getType() == Notification.NotifType.warning));
        }

        @Test
        @DisplayName("✅ send() with null link should default link to 'overview'")
        void send_nullLink_defaultsToOverview() {
            given(notifRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            notifService.send(1, "Test", "message", Notification.NotifType.info, null);

            verify(notifRepo).save(argThat(n -> n.getLink().equals("overview")));
        }

        @Test
        @DisplayName("✅ send() with custom link should use that link")
        void send_customLink_usesProvidedLink() {
            given(notifRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            notifService.send(1, "Test", "msg", Notification.NotifType.info, "custom-page");

            verify(notifRepo).save(argThat(n -> n.getLink().equals("custom-page")));
        }
    }
}

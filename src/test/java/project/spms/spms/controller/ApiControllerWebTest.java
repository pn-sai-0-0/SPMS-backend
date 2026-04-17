package project.spms.spms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import project.spms.spms.controller.ApiController;
import project.spms.spms.dto.*;
import project.spms.spms.entity.*;
import project.spms.spms.repository.*;
import project.spms.spms.service.*;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  WEB LAYER TESTS — ApiController (MockMvc)                         │
 * │                                                                     │
 * │  Strategy: @WebMvcTest loads ONLY the web layer (controllers,       │
 * │  filters, MVC config). All services and repos are mocked.           │
 * │  Tests verify HTTP request/response mapping, status codes, and      │
 * │  JSON structure — not business logic (that's in unit tests).        │
 * │                                                                     │
 * │  Run: mvn test -Dtest=ApiControllerWebTest                          │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@WebMvcTest(ApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Web Layer Tests — ApiController")
class ApiControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Mock every dependency that ApiController @Autowires ──────────────────
    @MockBean private UserService userService;
    @MockBean private ProjectService projectService;
    @MockBean private NotificationService notifService;
    @MockBean private FileStorageService fileStorage;
    @MockBean private AuditService auditService;
    @MockBean private UserRepository userRepo;
    @MockBean private ProjectRepository projRepo;
    @MockBean private DepartmentRepository deptRepo;
    @MockBean private ProjectAssignmentRepository assignRepo;
    @MockBean private ProjectFileRepository fileRepo;
    @MockBean private ProjectCommentRepository commentRepo;
    @MockBean private ProjectHistoryRepository historyRepo;
    @MockBean private NotificationRepository notifRepo;
    @MockBean private AuditLogRepository auditRepo;
    @MockBean private DailyActivityRepository activityRepo;
    @MockBean private MessageRepository messageRepo;
    @MockBean private AppSettingRepository settingRepo;
    @MockBean private CompanyRepository companyRepo;

    // ── Fixture ───────────────────────────────────────────────────────────────
    private User mockEmployee;
    private Project mockProject;

    @BeforeEach
    void setUp() {
        mockEmployee = new User();
        mockEmployee.setId(1);
        mockEmployee.setUsername("john.doe");
        mockEmployee.setName("John Doe");
        mockEmployee.setRole(User.Role.employee);
        mockEmployee.setStatus(User.Status.active);
        mockEmployee.setDepartmentName("Engineering");
        mockEmployee.setEmployeeId("EMP-001");

        mockProject = new Project();
        mockProject.setId(1);
        mockProject.setCode("PROJ-001");
        mockProject.setName("E-Commerce Platform Redesign");
        mockProject.setStatus(Project.ProjectStatus.active);
        mockProject.setPriority(Project.Priority.high);
        mockProject.setProgress(60);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AUTH ENDPOINT TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("✅ Should return 200 with user data on valid credentials")
        void login_validCredentials_returns200WithUser() throws Exception {
            given(userService.login("john.doe", "employee", "password"))
                    .willReturn(Optional.of(mockEmployee));
            given(assignRepo.countByUserId(anyInt())).willReturn(1L);

            Map<String, String> body = Map.of(
                    "username", "john.doe",
                    "password", "password",
                    "role", "employee"
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("john.doe"))
                    .andExpect(jsonPath("$.data.role").value("employee"));
        }

        @Test
        @DisplayName("❌ Should return 200 with success=false on invalid credentials")
        void login_invalidCredentials_returnsErrorResponse() throws Exception {
            given(userService.login("john.doe", "employee", "wrongpass"))
                    .willReturn(Optional.empty());

            Map<String, String> body = Map.of(
                    "username", "john.doe",
                    "password", "wrongpass",
                    "role", "employee"
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid credentials")));
        }

        @Test
        @DisplayName("❌ Should handle service exception gracefully")
        void login_serviceThrows_returnsErrorResponse() throws Exception {
            given(userService.login(any(), any(), any()))
                    .willThrow(new RuntimeException("DB error"));

            Map<String, String> body = Map.of(
                    "username", "john.doe", "password", "pass", "role", "employee");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USERS ENDPOINT TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/users")
    class GetUsersTests {

        @Test
        @DisplayName("✅ Should return all users as list")
        void listUsers_noFilter_returnsAll() throws Exception {
            given(userService.getAll()).willReturn(List.of(mockEmployee));
            given(assignRepo.countByUserId(1)).willReturn(1L);

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].username").value("john.doe"));
        }

        @Test
        @DisplayName("✅ Should filter users by role=employee")
        void listUsers_roleFilter_returnsFiltered() throws Exception {
            given(userService.getByRole("employee")).willReturn(List.of(mockEmployee));
            given(assignRepo.countByUserId(anyInt())).willReturn(1L);

            mockMvc.perform(get("/api/users").param("role", "employee"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].role").value("employee"));
        }

        @Test
        @DisplayName("✅ Should filter users by managerId")
        void listUsers_managerIdFilter_returnsTeam() throws Exception {
            given(userService.getByManagerId(5)).willReturn(List.of(mockEmployee));
            given(assignRepo.countByUserId(anyInt())).willReturn(1L);

            mockMvc.perform(get("/api/users").param("managerId", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("✅ Should return user with daily activities")
        void getUser_existingId_returnsUserWithActivities() throws Exception {
            given(userRepo.findById(1)).willReturn(Optional.of(mockEmployee));
            given(assignRepo.countByUserId(1)).willReturn(2L);
            given(activityRepo.findByUserIdOrderByActivityDateDesc(1)).willReturn(List.of());

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.id").value(1))
                    .andExpect(jsonPath("$.data.dailyActivities").isArray());
        }

        @Test
        @DisplayName("❌ Should return error response for unknown id")
        void getUser_unknownId_returnsError() throws Exception {
            given(userRepo.findById(999)).willReturn(Optional.empty());

            mockMvc.perform(get("/api/users/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("not found")));
        }
    }

    @Nested
    @DisplayName("POST /api/users")
    class CreateUserTests {

        @Test
        @DisplayName("✅ Should create user and return 200")
        void createUser_validRequest_returns200() throws Exception {
            given(userService.create(any(), any(), any(), any())).willReturn(mockEmployee);

            Map<String, Object> body = new HashMap<>();
            body.put("username", "new.user");
            body.put("name", "New User");
            body.put("email", "new@spms.com");
            body.put("role", "employee");
            body.put("password", "password");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("❌ Should return error when username is taken")
        void createUser_duplicateUsername_returnsError() throws Exception {
            given(userService.create(any(), any(), any(), any()))
                    .willThrow(new RuntimeException("Username already taken: new.user"));

            Map<String, Object> body = Map.of("username", "new.user", "name", "X",
                    "email", "x@x.com", "role", "employee", "password", "p");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Username already taken")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROJECT ENDPOINT TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/projects")
    class GetProjectsTests {

        @Test
        @DisplayName("✅ Should return all projects")
        void listProjects_noFilter_returnsAll() throws Exception {
            given(projectService.getAll()).willReturn(List.of(mockProject));

            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].code").value("PROJ-001"));
        }

        @Test
        @DisplayName("✅ Should filter projects by userId")
        void listProjects_userIdFilter_returnsAssigned() throws Exception {
            given(projectService.getByUserId(1)).willReturn(List.of(mockProject));

            mockMvc.perform(get("/api/projects").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].code").value("PROJ-001"));
        }
    }

    @Nested
    @DisplayName("PUT /api/projects/{id}/progress")
    class UpdateProgressTests {

        @Test
        @DisplayName("✅ Should update progress successfully")
        void updateProgress_validInput_returns200() throws Exception {
            willDoNothing().given(projectService)
                    .updateProgress(anyInt(), anyInt(), any(), any(), any());

            Map<String, Object> body = Map.of("progress", 75, "note", "Good progress",
                    "userId", 1, "userName", "John Doe");

            mockMvc.perform(put("/api/projects/1/progress")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION ENDPOINT TESTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/notifications")
    class NotificationTests {

        @Test
        @DisplayName("✅ Should return notifications for user")
        void getNotifications_returnsUserNotifications() throws Exception {
            Notification n = new Notification();
            n.setId(1);
            n.setUserId(1);
            n.setTitle("Test");
            n.setIsRead(false);

            given(notifRepo.findByUserIdOrderByCreatedAtDesc(1)).willReturn(List.of(n));
            given(notifRepo.countByUserIdAndIsReadFalse(1)).willReturn(1L);

            mockMvc.perform(get("/api/notifications").param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notifications", hasSize(1)))
                    .andExpect(jsonPath("$.data.unreadCount").value(1));
        }
    }
}

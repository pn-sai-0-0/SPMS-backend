package project.spms.spms.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  SECURITY TESTS — SecurityConfig, BCrypt, CSRF, CORS               │
 * │                                                                     │
 * │  Strategy: @SpringBootTest + @AutoConfigureMockMvc.                │
 * │  Tests verify the Spring Security configuration behaves exactly     │
 * │  as SPMS expects: CSRF disabled, all routes open, BCrypt wired     │
 * │  correctly.                                                         │
 * │                                                                     │
 * │  Run: mvn test -Dtest=SecurityConfigTest                            │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Tests — Spring Security Configuration")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    // ══════════════════════════════════════════════════════════════════════════
    // ENDPOINT ACCESS CONTROL — all routes must be accessible (permitAll)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Endpoint Access — all API routes should be publicly accessible")
    class EndpointAccessTests {

        @Test
        @DisplayName("✅ /api/auth/login should be accessible without authentication")
        void loginEndpoint_isPubliclyAccessible() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"x\",\"password\":\"y\",\"role\":\"employee\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ /api/users (GET) should be accessible without any auth token")
        void usersEndpoint_noAuthRequired() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ /api/projects (GET) should be accessible without any auth token")
        void projectsEndpoint_noAuthRequired() throws Exception {
            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ /api/notifications (GET) should be accessible without auth")
        void notificationsEndpoint_noAuthRequired() throws Exception {
            mockMvc.perform(get("/api/notifications").param("userId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ /api/dashboard/stats (GET) should be accessible without auth")
        void dashboardStats_noAuthRequired() throws Exception {
            mockMvc.perform(get("/api/dashboard/stats").param("userId", "1").param("role", "employee"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ Static resources (index.html) should be served without auth")
        void staticResources_served_withoutAuth() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CSRF — CSRF protection must be DISABLED (React SPA does not send tokens)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CSRF Protection — must be disabled for SPA")
    class CsrfTests {

        @Test
        @DisplayName("✅ POST without CSRF token should succeed (CSRF is disabled)")
        void post_withoutCsrfToken_succeeds() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"x\",\"password\":\"y\",\"role\":\"employee\"}")
                    )
                    .andExpect(status().isOk()); // must be 200, not 403
        }

        @Test
        @DisplayName("✅ PUT without CSRF token should succeed (CSRF is disabled)")
        void put_withoutCsrfToken_succeeds() throws Exception {
            // FIX 4: StatusResultMatchers has no isNotEqualTo(int).
            // Use a result handler with AssertJ to verify the status is not 403.
            mockMvc.perform(put("/api/users/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"active\",\"requestorId\":1,\"requestorName\":\"Admin\"}")
                    )
                    .andExpect(result ->
                            assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        }

        @Test
        @DisplayName("✅ DELETE without CSRF token should succeed (CSRF is disabled)")
        void delete_withoutCsrfToken_succeeds() throws Exception {
            // FIX 5: A non-existent user returns a JSON error response (200 with success=false),
            // so we verify the security layer doesn't block with a 403 — not that it returns 200.
            mockMvc.perform(delete("/api/users/9999"))
                    .andExpect(result ->
                            assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BCRYPT PASSWORD ENCODER
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("BCryptPasswordEncoder — bean is correctly configured")
    class BCryptTests {

        @Test
        @DisplayName("✅ BCryptPasswordEncoder bean is wired in context")
        void bcryptEncoder_beanExists() {
            assertThat(passwordEncoder).isNotNull();
        }

        @Test
        @DisplayName("✅ encode() produces a BCrypt hash (starts with $2a$)")
        void encode_producesValidBcryptHash() {
            String hash = passwordEncoder.encode("password");
            assertThat(hash).startsWith("$2a$");
        }

        @Test
        @DisplayName("✅ encode() produces different hashes for the same password (salted)")
        void encode_isSalted_differentsHashesEachTime() {
            String hash1 = passwordEncoder.encode("password");
            String hash2 = passwordEncoder.encode("password");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("✅ matches() correctly verifies a password against its hash")
        void matches_correctPassword_returnsTrue() {
            String hash = passwordEncoder.encode("mysecretpassword");
            assertThat(passwordEncoder.matches("mysecretpassword", hash)).isTrue();
        }

        @Test
        @DisplayName("❌ matches() returns false for wrong password")
        void matches_wrongPassword_returnsFalse() {
            String hash = passwordEncoder.encode("correctpassword");
            assertThat(passwordEncoder.matches("wrongpassword", hash)).isFalse();
        }

        @Test
        @DisplayName("❌ matches() returns false for empty string against real hash")
        void matches_emptyPassword_returnsFalse() {
            String hash = passwordEncoder.encode("realpassword");
            assertThat(passwordEncoder.matches("", hash)).isFalse();
        }

        @Test
        @DisplayName("✅ BCrypt strength is 10 rounds (industry standard minimum)")
        void encode_strengthIs10Rounds() {
            String hash = passwordEncoder.encode("test");
            assertThat(hash).contains("$2a$10$");
        }

        @Test
        @DisplayName("✅ 'password' string encodes and verifies correctly (default seed data password)")
        void defaultPassword_encodesAndVerifies() {
            String hash = passwordEncoder.encode("password");
            assertThat(passwordEncoder.matches("password", hash)).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HTTP RESPONSE SECURITY HEADERS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HTTP Response — Security headers")
    class SecurityHeaderTests {

        @Test
        @DisplayName("✅ API response should NOT contain WWW-Authenticate header (no HTTP Basic)")
        void apiResponse_noWwwAuthenticate() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        @DisplayName("✅ API responses should include Content-Type application/json")
        void apiResponse_hasJsonContentType() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CORS — Cross-Origin Resource Sharing (React frontend on different origin)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CORS — must allow cross-origin requests from React frontend")
    class CorsTests {

        @Test
        @DisplayName("✅ Preflight OPTIONS request should be allowed from any origin")
        void preflightOptions_allowsAnyOrigin() throws Exception {
            mockMvc.perform(options("/api/auth/login")
                            .header("Origin", "https://spms010.netlify.app")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("✅ GET request with Origin header should include CORS Allow-Origin in response")
        void getWithOrigin_hasCorsHeader() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Origin", "https://spms010.netlify.app"))
                    .andExpect(header().exists("Access-Control-Allow-Origin"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INPUT VALIDATION — API should handle malformed input gracefully
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Input Handling — malformed requests should not cause 500 errors")
    class InputHandlingTests {

        @Test
        @DisplayName("✅ Empty login body should return 200 with error JSON, not 400/500")
        void login_emptyBody_returnsErrorJsonNot500() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("✅ Non-numeric user id path variable returns JSON error, not 500")
        void getUser_stringId_returnsHandledError() throws Exception {
            mockMvc.perform(get("/api/users/not-a-number"))
                    .andExpect(status().is4xxClientError());
        }
    }
}

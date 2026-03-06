package project.spms.spms.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role; // "employee" | "manager" | "hr" | "admin"
}
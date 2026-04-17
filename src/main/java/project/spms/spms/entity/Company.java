package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Company entity — added for Bug Fix #8.
 * Instead of registering individual users, organisations register as a Company.
 * Each company gets one admin user created automatically at registration time.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "company_name", nullable = false, unique = true, length = 200)
    private String companyName;

    @Column(name = "company_email", nullable = false, unique = true, length = 150)
    private String companyEmail;

    @Column(name = "company_phone", length = 30)
    private String companyPhone;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "website", length = 200)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CompanyStatus status = CompanyStatus.active;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CompanyStatus {
        active, inactive, pending
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

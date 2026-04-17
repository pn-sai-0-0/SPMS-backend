package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "project_files")
public class ProjectFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "project_id", nullable = false)
    private Integer projectId;
    @Column(nullable = false, length = 255)
    private String filename;
    @Column(nullable = false, length = 500)
    private String filepath;
    private String filesize;
    @Column(name = "uploader_id")
    private Integer uploaderId;
    @Transient
    private String uploaderName;
    @Column(name = "upload_date", updatable = false)
    private LocalDateTime uploadDate;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
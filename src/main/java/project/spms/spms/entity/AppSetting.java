package project.spms.spms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "app_settings")
public class AppSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "setting_key", unique = true, nullable = false, length = 100)
    private String settingKey;
    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;
}
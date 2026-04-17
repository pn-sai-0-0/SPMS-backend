package project.spms.spms.repository;

import project.spms.spms.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, Integer> {
    Optional<AppSetting> findBySettingKey(String key);
}
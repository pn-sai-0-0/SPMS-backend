package project.spms.spms.repository;

import project.spms.spms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<AuditLog> findByTypeOrderByCreatedAtDesc(AuditLog.AuditType type);
}
package project.spms.spms.service;

import project.spms.spms.entity.AuditLog;
import project.spms.spms.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditRepo;

    @Transactional
    public void log(Integer userId, String userName, String action, AuditLog.AuditType type) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setUserName(userName);
        entry.setAction(action);
        entry.setType(type);
        auditRepo.save(entry);
    }

    @Transactional
    public void log(String userName, String action, AuditLog.AuditType type) {
        log(null, userName, action, type);
    }
}
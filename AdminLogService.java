package com.example.project2.service;

import com.example.project2.model.AdminLog;
import com.example.project2.model.AppUser;
import com.example.project2.repository.AdminLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminLogService {
    private final AdminLogRepository repo;
    public AdminLogService(AdminLogRepository repo) { this.repo = repo; }

    public void log(AppUser user, String action, String entityType, Long entityId, String details) {
        try {
            AdminLog log = new AdminLog();
            log.setUser(user);
            log.setUserEmail(user != null ? user.getEmail() : null);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDetails(details);
            repo.save(log);
        } catch (Exception ignored) {}
    }

    public void log(String userEmail, String action, String entityType, Long entityId, String details) {
        try {
            AdminLog log = new AdminLog();
            log.setUserEmail(userEmail);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDetails(details);
            repo.save(log);
        } catch (Exception ignored) {}
    }
}

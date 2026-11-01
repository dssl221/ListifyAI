package com.listifyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.entity.AdminAuditLog;
import com.listifyai.entity.User;
import com.listifyai.repository.AdminAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository, ObjectMapper objectMapper) {
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void log(User admin, String action, String targetType, UUID targetId, Object details, HttpServletRequest request) {
        AdminAuditLog entry = new AdminAuditLog();
        entry.setAdminUser(admin);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        try {
            entry.setDetails(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            entry.setDetails("{}");
        }
        entry.setIpAddress(extractIp(request));
        entry.setUserAgent(request.getHeader("User-Agent"));
        adminAuditLogRepository.save(entry);
    }

    private String extractIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = req.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = req.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}

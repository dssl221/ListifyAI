package com.listifyai.controller;

import com.listifyai.dto.AdminDtos.*;
import com.listifyai.entity.AdminAuditLog;
import com.listifyai.entity.User;
import com.listifyai.repository.AdminAuditLogRepository;
import com.listifyai.security.AdminOnly;
import com.listifyai.security.AuthUtils;
import com.listifyai.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@AdminOnly
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;
    private final AdminSupportService adminSupportService;
    private final AdminMonitoringService adminMonitoringService;
    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminController(AdminDashboardService adminDashboardService,
                           AdminUserService adminUserService,
                           AdminSupportService adminSupportService,
                           AdminMonitoringService adminMonitoringService,
                           AdminAuditLogRepository adminAuditLogRepository) {
        this.adminDashboardService = adminDashboardService;
        this.adminUserService = adminUserService;
        this.adminSupportService = adminSupportService;
        this.adminMonitoringService = adminMonitoringService;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @GetMapping("/dashboard")
    public AdminDashboardDTO dashboard() {
        return adminDashboardService.getDashboard();
    }

    @GetMapping("/users")
    public Page<AdminUserListItemDTO> users(@RequestParam(defaultValue = "") String search,
                                            @RequestParam(defaultValue = "") String plan,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return adminUserService.getAllUsers(search, plan, page, size);
    }

    @GetMapping("/users/{id}")
    public AdminUserDetailDTO userDetail(@PathVariable UUID id) {
        return adminUserService.getUserDetail(id);
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID id, HttpServletRequest request) {
        adminUserService.suspendUser(id, AuthUtils.getCurrentUser(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable UUID id, HttpServletRequest request) {
        adminUserService.reactivateUser(id, AuthUtils.getCurrentUser(), request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/plan")
    public ResponseEntity<Void> changePlan(@PathVariable UUID id,
                                           @Valid @RequestBody ChangePlanRequest requestBody,
                                           HttpServletRequest request) {
        adminUserService.changePlan(id, requestBody.plan(), AuthUtils.getCurrentUser(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/make-admin")
    public ResponseEntity<Void> makeAdmin(@PathVariable UUID id, HttpServletRequest request) {
        adminUserService.makeAdmin(id, AuthUtils.getCurrentUser(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tickets")
    public Page<AdminTicketDTO> tickets(@RequestParam(defaultValue = "") String status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return adminSupportService.getTickets(status, page, size);
    }

    @GetMapping("/tickets/count")
    public Map<String, Long> ticketCount() {
        return Map.of("open", adminSupportService.getOpenTicketCount(), "urgent", 0L);
    }

    @PutMapping("/tickets/{id}")
    public AdminTicketDTO updateTicket(@PathVariable UUID id,
                                       @RequestBody TicketUpdateRequest requestBody,
                                       HttpServletRequest request) {
        return adminSupportService.updateTicket(id, requestBody, AuthUtils.getCurrentUser(), request);
    }

    @GetMapping("/monitoring/overview")
    public AdminMonitoringOverviewDTO monitoringOverview() {
        return adminMonitoringService.getOverview();
    }

    @PostMapping("/monitoring/jobs/{id}/retry")
    public ResponseEntity<Void> retryMonitoringJob(@PathVariable UUID id, HttpServletRequest request) {
        adminMonitoringService.retryFailedJob(id, AuthUtils.getCurrentUser(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/monitoring/listings/{id}/pause")
    public ResponseEntity<Void> pauseMonitoring(@PathVariable UUID id, HttpServletRequest request) {
        adminMonitoringService.pauseMonitoredListing(id, AuthUtils.getCurrentUser(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/monitoring/listings/{id}/resume")
    public ResponseEntity<Void> resumeMonitoring(@PathVariable UUID id, HttpServletRequest request) {
        adminMonitoringService.resumeMonitoredListing(id, AuthUtils.getCurrentUser(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/claude-usage")
    public AdminClaudeUsageDTO claudeUsage() {
        return adminDashboardService.getClaudeUsage();
    }

    @GetMapping("/audit-logs")
    public Page<AdminAuditLogDTO> auditLogs(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return adminAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).map(this::toAuditDto);
    }

    private AdminAuditLogDTO toAuditDto(AdminAuditLog log) {
        User admin = log.getAdminUser();
        return new AdminAuditLogDTO(log.getId(), admin == null ? null : admin.getEmail(), log.getAction(),
                log.getTargetType(), log.getTargetId(), log.getDetails(), log.getIpAddress(), log.getCreatedAt());
    }
}

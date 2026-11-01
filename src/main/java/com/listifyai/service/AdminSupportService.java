package com.listifyai.service;

import com.listifyai.dto.AdminDtos.*;
import com.listifyai.entity.SupportTicket;
import com.listifyai.entity.User;
import com.listifyai.repository.SupportTicketRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@SuppressWarnings({"null", "unchecked", "unused"})
public class AdminSupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final NotificationService notificationService;
    private final AdminAuditService adminAuditService;

    public AdminSupportService(SupportTicketRepository supportTicketRepository,
                               NotificationService notificationService,
                               AdminAuditService adminAuditService) {
        this.supportTicketRepository = supportTicketRepository;
        this.notificationService = notificationService;
        this.adminAuditService = adminAuditService;
    }

    public AdminTicketDTO createTicket(User user, TicketSubmitRequest req) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setType(req.type());
        ticket.setSubject(req.subject());
        ticket.setDescription(req.description());
        SupportTicket saved = supportTicketRepository.save(ticket);
        notificationService.sendSupportTicketCreated(saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdminTicketDTO> getTickets(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets = status == null || status.isBlank()
                ? supportTicketRepository.findAllByOrderByCreatedAtDesc(pageable)
                : supportTicketRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return tickets.map(this::toDto);
    }

    public AdminTicketDTO updateTicket(UUID ticketId, TicketUpdateRequest req, User admin, HttpServletRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (req.status() != null) ticket.setStatus(req.status());
        if (req.priority() != null) ticket.setPriority(req.priority());
        if (req.adminNotes() != null) ticket.setAdminNotes(req.adminNotes());
        ticket.setUpdatedAt(OffsetDateTime.now());
        SupportTicket saved = supportTicketRepository.save(ticket);
        adminAuditService.log(admin, "TICKET_UPDATED", "support_ticket", ticketId,
                Map.of("status", saved.getStatus(), "priority", saved.getPriority()), request);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public long getOpenTicketCount() {
        return supportTicketRepository.countByStatusIn(List.of("open", "in_progress"));
    }

    private AdminTicketDTO toDto(SupportTicket ticket) {
        User user = ticket.getUser();
        return new AdminTicketDTO(ticket.getId(), user.getId(), user.getEmail(), ticket.getType(),
                ticket.getSubject(), ticket.getDescription(), ticket.getStatus(), ticket.getPriority(),
                ticket.getAdminNotes(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }
}

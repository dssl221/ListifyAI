package com.listifyai.controller;

import com.listifyai.dto.AdminDtos.AdminTicketDTO;
import com.listifyai.dto.AdminDtos.TicketSubmitRequest;
import com.listifyai.security.AuthUtils;
import com.listifyai.service.AdminSupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final AdminSupportService adminSupportService;

    public SupportController(AdminSupportService adminSupportService) {
        this.adminSupportService = adminSupportService;
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTicketDTO createTicket(@Valid @RequestBody TicketSubmitRequest request) {
        return adminSupportService.createTicket(AuthUtils.getCurrentUser(), request);
    }
}

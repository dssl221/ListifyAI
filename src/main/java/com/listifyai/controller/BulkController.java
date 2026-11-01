package com.listifyai.controller;

import com.listifyai.dto.BulkJobStatusDTO;
import com.listifyai.entity.User;
import com.listifyai.security.AuthUtils;
import com.listifyai.service.BulkJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/bulk")
public class BulkController {

    private final BulkJobService bulkJobService;

    public BulkController(BulkJobService bulkJobService) {
        this.bulkJobService = bulkJobService;
    }

    /**
     * POST /api/bulk/upload
     * Accepts a CSV file (max 2 MB, max 50 rows).
     * Returns 202 Accepted immediately — processing runs in background.
     */
    @PostMapping("/upload")
    public ResponseEntity<BulkJobStatusDTO> upload(
            @RequestParam("file") MultipartFile file) throws IOException {

        // Content-type validation
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("text/csv") &&
                 !contentType.equals("application/vnd.ms-excel") &&
                 !contentType.equals("application/octet-stream"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "File must be a CSV.");
        }

        // Size guard (belt-and-suspenders alongside service-level check)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "File exceeds 2 MB limit.");
        }

        User user = AuthUtils.getCurrentUser();
        BulkJobStatusDTO status = bulkJobService.uploadAndQueue(file, user);
        return ResponseEntity.accepted().body(status);
    }

    /**
     * GET /api/bulk/status/{jobId}
     * Polls the status and progress of a previously submitted bulk job.
     * Returns 403 if the job belongs to a different user.
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<BulkJobStatusDTO> status(@PathVariable UUID jobId) {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(bulkJobService.getStatus(jobId, user));
    }

    /**
     * GET /api/bulk/jobs
     * Gets all bulk jobs for the current user.
     */
    @GetMapping("/jobs")
    public ResponseEntity<java.util.List<BulkJobStatusDTO>> getJobs() {
        User user = AuthUtils.getCurrentUser();
        return ResponseEntity.ok(bulkJobService.getJobs(user));
    }
}

package com.listifyai.dto;

import java.util.UUID;

public record BulkJobStatusDTO(
        UUID jobId,
        String status,
        int totalRows,
        int processedRows,
        int failedRows,
        int progressPercent,
        String resultCsvUrl
) {}

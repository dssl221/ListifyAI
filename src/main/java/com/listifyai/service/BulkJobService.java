package com.listifyai.service;

import com.listifyai.dto.BulkJobStatusDTO;
import com.listifyai.dto.ProductInputDTO;
import com.listifyai.entity.BulkJob;
import com.listifyai.entity.User;
import com.listifyai.exception.ListingLimitException;
import com.listifyai.repository.BulkJobRepository;
import com.listifyai.security.AuthUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.listifyai.entity.BulkJobItem;
import com.listifyai.repository.BulkJobItemRepository;

@Service
public class BulkJobService {

    private static final int MAX_ROWS = 50;
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    private static final String COL_PRODUCT_NAME = "productName";
    private static final String COL_MATERIAL = "material";
    private static final String COL_SIZE = "size";
    private static final String COL_TARGET_BUYER = "targetBuyer";
    private static final String COL_USE_CASE = "useCase";
    private static final String COL_OCCASION = "occasion";
    private static final String COL_PLATFORM = "platform";
    private static final String COL_TONE = "tone";

    private static final List<String> REQUIRED_COLUMNS = List.of(
            COL_PRODUCT_NAME,
            COL_MATERIAL,
            COL_SIZE,
            COL_TARGET_BUYER,
            COL_USE_CASE,
            COL_OCCASION,
            COL_PLATFORM,
            COL_TONE
    );

    private final BulkJobRepository bulkJobRepository;
    private final BulkJobItemRepository bulkJobItemRepository;
    private final UsageService usageService;
    private final ObjectMapper objectMapper;

    public BulkJobService(BulkJobRepository bulkJobRepository,
                          BulkJobItemRepository bulkJobItemRepository,
                          UsageService usageService,
                          ObjectMapper objectMapper) {
        this.bulkJobRepository = bulkJobRepository;
        this.bulkJobItemRepository = bulkJobItemRepository;
        this.usageService = usageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BulkJobStatusDTO uploadAndQueue(MultipartFile file, User user) throws IOException {
        AuthUtils.assertNotSuspended(user);

        if (!"pro".equalsIgnoreCase(user.getPlan())) {
            throw new ListingLimitException("Bulk Upload requires Pro plan. Upgrade to continue.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 2 MB limit.");
        }

        List<ProductInputDTO> rows = parseCsv(file);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file contains no data rows.");
        }
        if (rows.size() > MAX_ROWS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CSV exceeds maximum of " + MAX_ROWS + " rows (found " + rows.size() + ")."
            );
        }

        String remainingStr = usageService.getRemainingCredits(user);
        if (!"Unlimited".equals(remainingStr)) {
            int remaining = Integer.parseInt(remainingStr);
            if (rows.size() > remaining) {
                throw new ListingLimitException("Not enough credits. You have " + remaining + " remaining credits, but CSV contains " + rows.size() + " rows.");
            }
        }

        BulkJob job = new BulkJob();
        job.setUser(user);
        job.setStatus("PENDING");
        job.setTotalItems(rows.size());
        job.setCompletedItems(0);
        job.setFailedItems(0);
        job = bulkJobRepository.save(job);
        LoggerFactory.getLogger(BulkJobService.class).info("Job {} started for user {}", job.getId(), user.getId());

        List<BulkJobItem> items = new ArrayList<>();
        for (ProductInputDTO row : rows) {
            BulkJobItem item = new BulkJobItem();
            item.setJob(job);
            item.setStatus("PENDING");
            try {
                item.setInputPayload(objectMapper.writeValueAsString(row));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize row");
            }
            items.add(item);
        }
        bulkJobItemRepository.saveAll(items);

        return toStatusDTO(job);
    }

    public BulkJobStatusDTO getStatus(UUID jobId, User user) {
        @SuppressWarnings("null")
        BulkJob job = bulkJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + jobId.toString()));

        if (!job.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this job.");
        }

        return toStatusDTO(job);
    }

    public List<BulkJobStatusDTO> getJobs(User user) {
        return bulkJobRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toStatusDTO)
                .toList();
    }

    List<ProductInputDTO> parseCsv(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            Set<String> headers = parser.getHeaderMap().keySet();
            List<String> missingColumns = REQUIRED_COLUMNS.stream()
                    .filter(required -> !headers.contains(required))
                    .toList();

            if (!missingColumns.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing required CSV columns: " + String.join(", ", missingColumns)
                );
            }

            List<ProductInputDTO> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(new ProductInputDTO(
                        get(record, COL_PRODUCT_NAME),
                        get(record, COL_MATERIAL),
                        get(record, COL_SIZE),
                        get(record, COL_TARGET_BUYER),
                        get(record, COL_USE_CASE),
                        get(record, COL_OCCASION),
                        get(record, COL_PLATFORM),
                        get(record, COL_TONE),
                        null
                ));
            }
            return rows;
        }
    }

    private BulkJobStatusDTO toStatusDTO(BulkJob job) {
        int percent = job.getTotalItems() > 0 ? (job.getCompletedItems() * 100) / job.getTotalItems() : 0;
        return new BulkJobStatusDTO(
                job.getId(),
                job.getStatus(),
                job.getTotalItems(),
                job.getCompletedItems(),
                job.getFailedItems(),
                percent,
                job.getResultCsvUrl()
        );
    }

    private String get(CSVRecord record, String column) {
        try {
            String value = record.get(column);
            return (value != null && !value.isBlank()) ? value.trim() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

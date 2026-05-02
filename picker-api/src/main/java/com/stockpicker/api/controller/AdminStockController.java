package com.stockpicker.api.controller;

import com.stockpicker.api.service.CsvImportWorker;
import com.stockpicker.common.entity.DataImportJob;
import com.stockpicker.common.enums.DataImportStatus;
import com.stockpicker.common.repository.DataImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/stocks")
@RequiredArgsConstructor
public class AdminStockController {

    private final DataImportJobRepository jobRepository;
    private final CsvImportWorker csvImportWorker;

    @PostMapping("/import")
    public ResponseEntity<UUID> importStocks(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        DataImportJob job = DataImportJob.builder()
                .fileName(file.getOriginalFilename())
                .status(DataImportStatus.PENDING)
                .build();
        
        job = jobRepository.save(job);

        // Copy MultipartFile to a temporary file
        File tempFile = File.createTempFile("nse_stocks_", ".csv");
        try {
            Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("MultipartFile saved to temp file: {}", tempFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save multipart file to temp file", e);
            job.setStatus(DataImportStatus.FAILED);
            job.setErrorMessage("Internal error: Failed to prepare file for processing.");
            jobRepository.save(job);
            return ResponseEntity.internalServerError().build();
        }

        // Trigger async processing
        csvImportWorker.importCsv(job.getId(), tempFile);

        return ResponseEntity.accepted().body(job.getId());
    }

    @GetMapping("/import/{jobId}")
    public ResponseEntity<DataImportJob> getImportStatus(@PathVariable UUID jobId) {
        return jobRepository.findById(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

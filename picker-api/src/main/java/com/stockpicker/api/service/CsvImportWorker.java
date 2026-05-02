package com.stockpicker.api.service;

import com.stockpicker.common.entity.DataImportJob;
import com.stockpicker.common.entity.Stock;
import com.stockpicker.common.enums.DataImportStatus;
import com.stockpicker.common.repository.DataImportJobRepository;
import com.stockpicker.common.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportWorker {

    private final DataImportJobRepository jobRepository;
    private final StockRepository stockRepository;

    @Async
    @Transactional
    public void importCsv(UUID jobId, File file) {
        DataImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Job not found: {}", jobId);
            return;
        }

        job.setStatus(DataImportStatus.PROCESSING);
        jobRepository.save(job);

        int totalRecords = 0;
        int successfulRecords = 0;
        int failedRecords = 0;
        List<Stock> stockBatch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }

                totalRecords++;
                try {
                    // Regex to handle commas inside quotes
                    String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    
                    // Strip surrounding quotes
                    for (int i = 0; i < columns.length; i++) {
                        columns[i] = columns[i].replaceAll("^\"|\"$", "").trim();
                    }

                    if (columns.length >= 3) {
                        String symbol = columns[0];
                        String name = columns[1];
                        String series = columns[2];

                        if ("EQ".equalsIgnoreCase(series)) {
                            Stock stock = Stock.builder()
                                    .ticker(symbol + ".NS")
                                    .companyName(name)
                                    .exchange("NSE")
                                    .build();
                            
                            stockBatch.add(stock);
                            successfulRecords++;

                            if (stockBatch.size() >= 500) {
                                stockRepository.saveAll(stockBatch);
                                stockBatch.clear();
                            }
                        } else {
                            // Optionally skip or count as successful if it's just filtered out
                            // But usually, success means it was processed into the DB.
                        }
                    } else {
                        throw new IllegalArgumentException("Malformed CSV row: insufficient columns");
                    }

                } catch (Exception e) {
                    log.warn("Failed to process row {}: {}", totalRecords, e.getMessage());
                    failedRecords++;
                }
            }

            // Save final batch
            if (!stockBatch.isEmpty()) {
                stockRepository.saveAll(stockBatch);
            }

            job.setStatus(DataImportStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Unrecoverable error during CSV import for job {}: {}", jobId, e.getMessage());
            job.setStatus(DataImportStatus.FAILED);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 1997) + "...";
            }
            job.setErrorMessage(errorMsg);
        } finally {
            job.setTotalRecords(totalRecords);
            job.setSuccessfulRecords(successfulRecords);
            job.setFailedRecords(failedRecords);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("Temporary file deleted: {}", file.getAbsolutePath());
                } else {
                    log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
                }
            }
        }
    }
}

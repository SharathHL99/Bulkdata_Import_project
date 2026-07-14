package com.bulkimport.controller;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.dto.ImportJobResponse;
import com.bulkimport.dto.ImportRecordResponse;
import com.bulkimport.dto.UploadResponse;
import com.bulkimport.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Tag(name = "Bulk Data Import",description = "APIs for CSV/Excel bulk data import processing")
public class ImportController {

    private final ImportService importService;

    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload CSV or Excel file")
    public ResponseEntity<UploadResponse> uploadFile(@RequestPart("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(importService.uploadFile(file));
    }


    @GetMapping("/jobs")
    @Operation(summary = "Get all import jobs")
    public ResponseEntity<List<ImportJobResponse>> getAllJobs() {

        return ResponseEntity.ok(importService.getAllImportJobs());

    }


    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get import job details")
    public ResponseEntity<ImportJobResponse> getJobById( @PathVariable Long jobId) {
        return ResponseEntity.ok(importService.getImportJobById(jobId));

    }


    @GetMapping("/jobs/{jobId}/failed-records")
    @Operation(summary = "Get failed import records")
    public ResponseEntity<List<ImportRecordResponse>> getFailedRecords( @PathVariable Long jobId) {
        return ResponseEntity.ok(importService.getFailedRecords(jobId));

    }


    @PostMapping("/jobs/{jobId}/retry")
    @Operation(summary = "Retry failed records")
    public ResponseEntity<String> retryFailedRecords(@PathVariable Long jobId) {

        importService.retryFailedRecords(jobId);

        return ResponseEntity.ok("Failed records retry started successfully.");

    }


    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Delete import job")
    public ResponseEntity<String> deleteJob(@PathVariable Long jobId) {

        importService.deleteImportJob(jobId);

        return ResponseEntity.ok("Import job deleted successfully."
        );

    }

}
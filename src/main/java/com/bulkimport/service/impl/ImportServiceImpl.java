package com.bulkimport.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.async.ImportAsyncProcessor;
import com.bulkimport.dto.ImportJobResponse;
import com.bulkimport.dto.ImportRecordResponse;
import com.bulkimport.dto.UploadResponse;
import com.bulkimport.entity.ImportJob;
import com.bulkimport.entity.ImportRecord;
import com.bulkimport.enums.ImportJobStatus;
import com.bulkimport.enums.ImportRecordStatus;
import com.bulkimport.exception.DuplicateFileException;
import com.bulkimport.exception.ImportJobNotFoundException;
import com.bulkimport.mapper.ImportMapper;
import com.bulkimport.repository.ImportJobRepository;
import com.bulkimport.repository.ImportRecordRepository;
import com.bulkimport.service.ImportService;
import com.bulkimport.util.FileHashUtil;
import com.bulkimport.validation.FileValidator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportServiceImpl implements ImportService {

    private final ImportJobRepository importJobRepository;

    private final ImportRecordRepository importRecordRepository;

    private final ImportMapper importMapper;

    private final FileValidator fileValidator;

    private final FileHashUtil fileHashUtil;

    private final ImportAsyncProcessor importAsyncProcessor;

    @Override
    public UploadResponse uploadFile(MultipartFile file) {

        fileValidator.validate(file);

        String fileHash = fileHashUtil.generateHash(file);

        if (importJobRepository.existsByFileHash(fileHash)) {
            throw new DuplicateFileException(
                    "File has already been imported.");
        }

        ImportJob importJob = ImportJob.builder()
                .fileName(file.getOriginalFilename())
                .fileHash(fileHash)
                .status(ImportJobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        importJob = importJobRepository.save(importJob);

        importAsyncProcessor.processFile(file, importJob.getId());

        return UploadResponse.builder()
                .jobId(importJob.getId())
                .fileName(importJob.getFileName())
                .status(importJob.getStatus())
                .message("File uploaded successfully. Import started.")
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportJobResponse> getAllImportJobs() {

        return importJobRepository.findAll()
                .stream()
                .map(importMapper::toImportJobResponse)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public ImportJobResponse getImportJobById(Long jobId) {

        ImportJob importJob = importJobRepository
                .findById(jobId)
                .orElseThrow(() ->
                        new ImportJobNotFoundException(
                                "Import job not found with id : " + jobId));

        return importMapper.toImportJobResponse(importJob);

    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ImportRecordResponse> getFailedRecords(Long jobId) {

        ImportJob importJob = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ImportJobNotFoundException(
                        "Import job not found with id : " + jobId));

        return importRecordRepository
                .findByImportJobAndStatus(
                        importJob,
                        ImportRecordStatus.INVALID)
                .stream()
                .map(importMapper::toImportRecordResponse)
                .toList();

    }

    @Override
    public void retryFailedRecords(Long jobId) {

        ImportJob importJob = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ImportJobNotFoundException(
                        "Import job not found with id : " + jobId));

        List<ImportRecord> failedRecords =
                importRecordRepository.findByImportJobAndStatus(
                        importJob,
                        ImportRecordStatus.INVALID);

        if (failedRecords.isEmpty()) {
            return;
        }

        for (ImportRecord record : failedRecords) {

            record.setStatus(ImportRecordStatus.VALID);
            record.setErrorMessage(null);

            importJob.setSuccessRecords(
                    importJob.getSuccessRecords() + 1);

            importJob.setFailedRecords(
                    importJob.getFailedRecords() - 1);

        }

        importRecordRepository.saveAll(failedRecords);

        if (importJob.getFailedRecords() == 0
                && importJob.getDuplicateRecords() == 0) {

            importJob.setStatus(ImportJobStatus.COMPLETED);

        } else {

            importJob.setStatus(ImportJobStatus.PARTIAL_SUCCESS);

        }

        importJobRepository.save(importJob);

    }
    
    @Override
    public void deleteImportJob(Long jobId) {

        ImportJob importJob = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ImportJobNotFoundException(
                        "Import job not found with id : " + jobId));

        importRecordRepository.deleteAll(
                importRecordRepository.findByImportJob(importJob));

        importJobRepository.delete(importJob);

    }
}
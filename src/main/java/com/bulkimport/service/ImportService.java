package com.bulkimport.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.dto.ImportJobResponse;
import com.bulkimport.dto.ImportRecordResponse;
import com.bulkimport.dto.UploadResponse;

public interface ImportService {

    UploadResponse uploadFile(MultipartFile file);

    List<ImportJobResponse> getAllImportJobs();

    ImportJobResponse getImportJobById(Long jobId);

    List<ImportRecordResponse> getFailedRecords(Long jobId);

    void retryFailedRecords(Long jobId);

    void deleteImportJob(Long jobId);

}
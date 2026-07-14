package com.bulkimport.dto;

import com.bulkimport.enums.ImportJobStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {

    private Long jobId;

    private String fileName;

    private ImportJobStatus status;

    private String message;

}
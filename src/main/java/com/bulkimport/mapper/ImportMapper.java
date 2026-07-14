package com.bulkimport.mapper;

import org.springframework.stereotype.Component;

import com.bulkimport.dto.ImportJobResponse;
import com.bulkimport.dto.ImportRecordResponse;
import com.bulkimport.dto.ImportSummaryResponse;
import com.bulkimport.entity.ImportJob;
import com.bulkimport.entity.ImportRecord;

@Component
public class ImportMapper {


    public ImportSummaryResponse toImportSummaryResponse(ImportJob job) {

        return ImportSummaryResponse.builder()
                .totalRecords(job.getTotalRecords())
                .successRecords(job.getSuccessRecords())
                .failedRecords(job.getFailedRecords())
                .duplicateRecords(job.getDuplicateRecords())
                .processingTime(job.getProcessingTime())
                .build();
    }


    public ImportJobResponse toImportJobResponse(ImportJob job) {

        return ImportJobResponse.builder()
                .id(job.getId())
                .fileName(job.getFileName())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .summary(toImportSummaryResponse(job))
                .build();

    }


    public ImportRecordResponse toImportRecordResponse(ImportRecord record) {

        return ImportRecordResponse.builder()
                .id(record.getId())
                .rowNumber(record.getRowNumber())
                .rawData(record.getRawData())
                .status(record.getStatus())
                .errorMessage(record.getErrorMessage())
                .createdAt(record.getCreatedAt())
                .build();

    }

}
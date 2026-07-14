package com.bulkimport.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportSummaryResponse {

    private Integer totalRecords;

    private Integer successRecords;

    private Integer failedRecords;

    private Integer duplicateRecords;

    private Long processingTime;

}
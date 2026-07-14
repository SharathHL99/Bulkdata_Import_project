package com.bulkimport.dto;

import java.time.LocalDateTime;
import com.bulkimport.enums.ImportJobStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportJobResponse {

    private Long id;

    private String fileName;

    private ImportJobStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private ImportSummaryResponse summary;

}
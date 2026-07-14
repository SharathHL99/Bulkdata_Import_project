package com.bulkimport.dto;

import java.time.LocalDateTime;
import com.bulkimport.enums.ImportRecordStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportRecordResponse {

    private Long id;

    private Integer rowNumber;

    private String rawData;

    private ImportRecordStatus status;

    private String errorMessage;

    private LocalDateTime createdAt;

}
package com.bulkimport.entity;

import java.time.LocalDateTime;
import com.bulkimport.enums.ImportRecordStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "import_records",
    indexes = {
        @Index(name = "idx_job_id", columnList = "job_id"),
        @Index(name = "idx_record_status", columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_row_number")
    private Integer rowNumber;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportRecordStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob importJob;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
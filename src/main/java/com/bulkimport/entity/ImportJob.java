package com.bulkimport.entity;

import java.time.LocalDateTime;

import com.bulkimport.enums.ImportJobStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "import_jobs",
        indexes = {
                @Index(name = "idx_file_hash", columnList = "fileHash", unique = true),
                @Index(name = "idx_job_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, unique = true, length = 64)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ImportJobStatus status = ImportJobStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalRecords = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer successRecords = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer failedRecords = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer duplicateRecords = 0;

    private Long processingTime;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

}
package com.bulkimport.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bulkimport.entity.ImportJob;
import com.bulkimport.enums.ImportJobStatus;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    Optional<ImportJob> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);

    Optional<ImportJob> findByFileName(String fileName);

    long countByStatus(ImportJobStatus status);

}
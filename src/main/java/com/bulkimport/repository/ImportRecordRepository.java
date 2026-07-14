package com.bulkimport.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bulkimport.entity.ImportJob;
import com.bulkimport.entity.ImportRecord;
import com.bulkimport.enums.ImportRecordStatus;

@Repository
public interface ImportRecordRepository extends JpaRepository<ImportRecord, Long> {

    List<ImportRecord> findByImportJob(ImportJob importJob);

    List<ImportRecord> findByImportJobAndStatus(ImportJob importJob,ImportRecordStatus status);

    long countByImportJob(ImportJob importJob);

    long countByImportJobAndStatus(ImportJob importJob,ImportRecordStatus status);

    boolean existsByImportJobAndRawData(ImportJob importJob,String rawData);

    List<ImportRecord> findAllByImportJob(ImportJob importJob);

}
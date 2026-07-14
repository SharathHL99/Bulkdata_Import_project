package com.bulkimport.async;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.constants.AppConstants;
import com.bulkimport.dto.ImportRecordDto;
import com.bulkimport.entity.ImportJob;
import com.bulkimport.entity.ImportRecord;
import com.bulkimport.enums.ImportJobStatus;
import com.bulkimport.enums.ImportRecordStatus;
import com.bulkimport.exception.FileProcessingException;
import com.bulkimport.repository.ImportJobRepository;
import com.bulkimport.repository.ImportRecordRepository;
import com.bulkimport.util.CsvReaderUtil;
import com.bulkimport.util.ExcelReaderUtil;
import com.bulkimport.validation.CsvValidator;
import com.bulkimport.validation.ExcelValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImportAsyncProcessor {

    private final ImportJobRepository importJobRepository;
    private final ImportRecordRepository importRecordRepository;
    private final CsvReaderUtil csvReaderUtil;
    private final ExcelReaderUtil excelReaderUtil;
    private final CsvValidator csvValidator;
    private final ExcelValidator excelValidator;
    private final Validator validator;

    private static final DataFormatter DATA_FORMATTER =new DataFormatter();

    public void processFile(MultipartFile file, Long jobId) {

    	System.out.println("Async processing started...");
    	System.out.println("Async processing completed...");

        long startTime = System.currentTimeMillis();

        ImportJob importJob = null;

        try {

            importJob = importJobRepository
                    .findById(jobId)
                    .orElseThrow(() ->
                            new FileProcessingException("Import job not found."));

            importJob.setStatus(ImportJobStatus.PROCESSING);

            importJobRepository.save(importJob);

            String fileName =file.getOriginalFilename();

            if (fileName == null) {

                throw new FileProcessingException("Invalid file name.");

            }

            if (fileName.toLowerCase()
                    .endsWith(AppConstants.CSV)) {

                processCsvFile(file,importJob);

            } else if (fileName.toLowerCase()
                    .endsWith(AppConstants.XLSX)) {

                processExcelFile(file,importJob);

            } else {

                throw new FileProcessingException("Only CSV and XLSX files are supported.");

            }

            importJob.setStatus(importJob.getFailedRecords() == 0&& importJob.getDuplicateRecords() == 0
                    ? ImportJobStatus.COMPLETED
                    : ImportJobStatus.PARTIAL_SUCCESS);

            importJob.setCompletedAt(LocalDateTime.now());

            importJob.setProcessingTime(System.currentTimeMillis() - startTime);

            importJobRepository.save(importJob);

        }

        catch (Exception ex) {

            ex.printStackTrace();

            if (importJob != null) {

                importJob.setStatus(ImportJobStatus.FAILED);
                importJob.setCompletedAt(LocalDateTime.now());
                importJob.setProcessingTime(System.currentTimeMillis() - startTime);

                importJobRepository.save(importJob);
            }

            return;
        }
    }

    

    private void processCsvFile( MultipartFile file,ImportJob importJob) {

        try (CSVParser parser =csvReaderUtil.readCsv(file)) {

            List<String> headers =new ArrayList<>(parser.getHeaderMap().keySet());

            csvValidator.validateHeaders(headers);

            List<ImportRecord> batch =new ArrayList<>();

            Set<String> processedRows =new HashSet<>();

            int rowNumber = 1;

            for (CSVRecord record : parser) {

                ImportRecordDto dto =ImportRecordDto.builder()
                                .name(record.get("Name").trim())
                                .email(record.get("Email").trim())
                                .phone(record.get("Phone").trim())
                                .salary(parseSalary(record.get("Salary")))
                                .department(record.get("Department").trim())
                                .build();

                processRecord(dto,rowNumber,importJob,batch,processedRows);

                rowNumber++;

                if (batch.size()>= AppConstants.BATCH_SIZE) {

                    importRecordRepository.saveAll(batch);

                    batch.clear();

                }

            }

            if (!batch.isEmpty()) {

                importRecordRepository.saveAll(batch);

            }

        }

        catch (IOException ex) {

            throw new FileProcessingException("Unable to process CSV file.",ex);

        }

    }


private void processExcelFile(MultipartFile file,ImportJob importJob) {

    Workbook workbook =excelReaderUtil.readWorkbook(file);

    try {

        Sheet sheet =workbook.getSheetAt(0);

        List<String> headers = List.of(getCellValue(sheet.getRow(0).getCell(0)),
                getCellValue(sheet.getRow(0).getCell(1)),
                getCellValue(sheet.getRow(0).getCell(2)),
                getCellValue(sheet.getRow(0).getCell(3)),
                getCellValue(sheet.getRow(0).getCell(4)));
        excelValidator.validateHeaders(headers);

        List<ImportRecord> batch =new ArrayList<>();

        Set<String> processedRows =new HashSet<>();

        int rowNumber = 1;

        for (Row row : sheet) {

            if (row.getRowNum() == 0) {
                continue;
            }

            ImportRecordDto dto =ImportRecordDto.builder()
                            .name(getCellValue(row.getCell(0)))
                            .email(getCellValue(row.getCell(1)))
                            .phone(getCellValue(row.getCell(2)))
                            .salary(parseSalary(getCellValue(row.getCell(3))))
                            .department(getCellValue(row.getCell(4)))
                            .build();

            processRecord(dto,rowNumber++,importJob,batch,processedRows);

            if (batch.size()>= AppConstants.BATCH_SIZE) {

                importRecordRepository.saveAll(batch);

                batch.clear();

            }

        }

        if (!batch.isEmpty()) {

            importRecordRepository.saveAll(batch);

        }

    }

    finally {

        try {

            workbook.close();

        }

        catch (IOException ignored) {

        }

    }

}

private void processRecord(ImportRecordDto dto,int rowNumber,ImportJob importJob,List<ImportRecord> batch,Set<String> processedRows) {

    importJob.setTotalRecords(importJob.getTotalRecords() + 1);

    String rawData =buildRawData(dto);

    ImportRecord record =new ImportRecord();

    record.setImportJob(importJob);
    record.setRowNumber(rowNumber);
    record.setRawData(rawData);
    record.setCreatedAt(LocalDateTime.now());

    if (!processedRows.add(rawData)
            || importRecordRepository.existsByImportJobAndRawData(importJob,rawData)) {

        record.setStatus(ImportRecordStatus.DUPLICATE);

        record.setErrorMessage("Record already exists.");

        importJob.setDuplicateRecords(importJob.getDuplicateRecords() + 1);

        batch.add(record);

        return;

    }

    Set<ConstraintViolation<ImportRecordDto>> violations =validator.validate(dto);

    if (!violations.isEmpty()) {

        record.setStatus(ImportRecordStatus.INVALID);

        record.setErrorMessage(buildErrorMessage(violations));

        importJob.setFailedRecords(importJob.getFailedRecords() + 1);

    }

    else {

        record.setStatus(ImportRecordStatus.VALID);

        record.setErrorMessage(null);

        importJob.setSuccessRecords(importJob.getSuccessRecords() + 1);

    }

    batch.add(record);

}

private String buildRawData(ImportRecordDto dto) {

    return String.join(",",

            safe(dto.getName()),

            safe(dto.getEmail()),

            safe(dto.getPhone()),

            dto.getSalary() == null
                    ? ""
                    : dto.getSalary().toString(),

            safe(dto.getDepartment()));

}

private String buildErrorMessage(Set<ConstraintViolation<ImportRecordDto>> violations) {

    StringBuilder builder =new StringBuilder();

    for (ConstraintViolation<ImportRecordDto> violation
            : violations) {

        if (builder.length() > 0) {

            builder.append(", ");

        }

        builder.append(violation.getMessage());

    }

    return builder.toString();

}

private Double parseSalary(String salary) {

    try {

        return Double.parseDouble(salary);

    }

    catch (Exception ex) {

        return -1.0;

    }

}

private String getCellValue(Cell cell) {

    if (cell == null) {

        return "";

    }

    return DATA_FORMATTER
            .formatCellValue(cell)
            .trim();

}

private String safe(String value) {

    return value == null
            ? ""
            : value.trim();

}

}
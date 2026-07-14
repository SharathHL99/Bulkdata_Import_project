package com.bulkimport.validation;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.exception.FileProcessingException;

@Component
public class FileValidator {
	
    private static final long MAX_FILE_SIZE =50 * 1024 * 1024;

    public void validate(MultipartFile file) {


        if (file == null || file.isEmpty()) {

            throw new FileProcessingException("File cannot be empty.");

        }

        String fileName = file.getOriginalFilename();


        if (fileName == null) {

            throw new FileProcessingException("Invalid file name.");

        }


        String lowerCaseName =fileName.toLowerCase();

        if (!(lowerCaseName.endsWith(".csv")|| lowerCaseName.endsWith(".xlsx"))) {

            throw new FileProcessingException("Only CSV and XLSX files are supported.");

        }


        if (file.getSize() > MAX_FILE_SIZE) {

            throw new FileProcessingException("File size should not exceed 50 MB.");

        }

    }

}
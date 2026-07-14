package com.bulkimport.util;

import java.io.IOException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.exception.FileProcessingException;

@Component
public class ExcelReaderUtil {

    public Workbook readWorkbook(MultipartFile file) {

        try {

            return new XSSFWorkbook(
                    file.getInputStream());

        } catch (IOException ex) {

            throw new FileProcessingException("Unable to read Excel file.",ex);

        }

    }

}
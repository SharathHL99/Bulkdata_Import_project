package com.bulkimport.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.exception.FileProcessingException;

@Component
public class CsvReaderUtil {

    public CSVParser readCsv(MultipartFile file) {

        try {

            Reader reader =new InputStreamReader(file.getInputStream());

            return CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

        } catch (IOException ex) {

            throw new FileProcessingException("Unable to read CSV file.",ex);

        }

    }

}
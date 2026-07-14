package com.bulkimport.validation;

import java.util.List;
import org.springframework.stereotype.Component;
import com.bulkimport.exception.FileProcessingException;

@Component
public class ExcelValidator {

    private static final List<String> REQUIRED_HEADERS =
            List.of("Name","Email","Phone","Salary","Department");

    public void validateHeaders(List<String> headers) {
        if (headers == null || headers.isEmpty()) {

            throw new FileProcessingException("Excel file header is missing.");

        }


        for (String requiredHeader : REQUIRED_HEADERS) {

            if (!headers.contains(requiredHeader)) {

                throw new FileProcessingException("Missing required column: "+ requiredHeader);

            }

        }

    }

}
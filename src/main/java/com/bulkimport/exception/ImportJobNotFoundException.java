package com.bulkimport.exception;

public class ImportJobNotFoundException extends RuntimeException {


    public ImportJobNotFoundException(String message) {
        super(message);
    }

}
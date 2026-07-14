package com.bulkimport.util;

import java.io.InputStream;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.bulkimport.exception.FileProcessingException;

@Component
public class FileHashUtil {


    public String generateHash(MultipartFile file) {

        try (InputStream inputStream =file.getInputStream()) {

            MessageDigest digest =MessageDigest.getInstance("SHA-256");

            byte[] bytes =new byte[8192];

            int count;

            while ((count = inputStream.read(bytes)) != -1) {

                digest.update(bytes, 0, count);

            }


            StringBuilder hash =new StringBuilder();

            for (byte b : digest.digest()) {

                hash.append(String.format("%02x", b));

            }

            return hash.toString();


        } catch (Exception ex) {

            throw new FileProcessingException("Unable to generate file hash.",ex);

        }

    }

}
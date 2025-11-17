package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.CreateUserRequest;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BulkUserUploadService {
    public Map<String, String> processBulkUpload(MultipartFile file, String tenantDbName, String hospitalId) throws IOException {
        String filename = file.getOriginalFilename();
        List<CreateUserRequest> userRequests;
        if (filename != null && filename.endsWith(".csv")) {
            userRequests = parseCSV(file);
        } else if (filename != null && filename.endsWith(".xlsx")) {
            userRequests = parseExcel(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format");
        }
    }
    private List<CreateUserRequest> parseCSV(MultipartFile file) throws IOException {
        List<CreateUserRequest> userRequests = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()){
                log.warn("Empty CSV file");
                throw new IllegalArgumentException("Empty CSV file");
            }
            // First row is header
            String[] headers = rows.getFirst();
            Map<String, Integer> headerMap = mapHeaders(headers);
            // Process data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                try {
                    CreateUserRequest user = parseUserFromRow(row, headerMap, i + 1);
                    userRequests.add(user);
                } catch (Exception e) {
                    log.warn("️ Skipping row {}: {}", i + 1, e.getMessage());
                }
            }
        }catch (CsvException e){
            log.warn("CSV read error");
            throw new IOException("CSV read error");
        }
        return userRequests;
    }

    private CreateUserRequest parseUserFromRow(String[] row, Map<String, Integer> headers, int rowNumber) {
        CreateUserRequest request = new CreateUserRequest();

        // Basic fields
        request.setFirstName(getStringValue(row, headers, "firstname"));
        request.setMiddleName(getStringValue(row, headers, "middlename"));
        request.setLastName(getStringValue(row, headers, "lastname"));
        request.setEmail(getStringValue(row, headers, "email"));
        request.setPhoneNumber(getStringValue(row, headers, "phonenumber"));

        String role = getStringValue(row, headers, "role");
        request.setRole(role != null ? role.toUpperCase() : null);

        // Parse role-specific details
        parseRoleSpecificDetails(request, row, headers);

        return request;
    }

    private Map<String, Integer> mapHeaders(String[] headers) {
        Map<String, Integer> headerMap = new HashMap<>();
        for(int i = 0; i<headers.length; i++){
            headerMap.put(headers[i].trim().toLowerCase(), i);
        }
        return headerMap;
    }

}

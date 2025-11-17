package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.CreateUserRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BulkUserUploadService {
    public Map<String, String> processBulkUpload(MultipartFile file, String tenantDbName, String hospitalId) {
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
        List<CreateUserRequest> users = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // First row is header
            String[] headers = rows.get(0);
            Map<String, Integer> headerMap = mapHeaders(headers);

            // Process data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                try {
                    CreateUserRequest user = parseUserFromRow(row, headerMap, i + 1);
                    users.add(user);
                } catch (Exception e) {
                    log.warn("⚠️ Skipping row {}: {}", i + 1, e.getMessage());
                }
            }

        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV file", e);
        }

        return users;
    }
}

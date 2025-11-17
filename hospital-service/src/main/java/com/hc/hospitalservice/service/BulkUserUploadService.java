package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.CreateUserRequest;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
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
        List<CreateUserRequest> users = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // First row is header
            String[] headers = rows.getFirst();
            Map<String, Integer> headerMap = mapHeaders(headers);

            // Process data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                try {
                    CreateUserRequest user = parseUserFromRow(row, headerMap, i + 1);
                    users.add(user);
                } catch (Exception e) {
                    log.warn(" Skipping row {}: {}", i + 1, e.getMessage());
                }
            }

        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV file", e);
        }

        return users;
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

    private void parseRoleSpecificDetailsFromExcel(CreateUserRequest request, Row row, Map<String, Integer> headers) {
        String role = request.getRole();

        if ("DOCTOR".equals(role)) {
            CreateUserRequest.DoctorDetailsRequest details = new CreateUserRequest.DoctorDetailsRequest();
            details.setSpecialization(getCellStringValue(row, headers, "specialization"));
            details.setDepartment(getCellStringValue(row, headers, "department"));
            details.setLicenseNumber(getCellStringValue(row, headers, "licensenumber"));
            details.setLicenseAuthority(getCellStringValue(row, headers, "licenseauthority"));
            details.setLicenseIssueDate(getCellDateValue(row, headers, "licenseissuedate"));
            details.setLicenseExpiryDate(getCellDateValue(row, headers, "licenseexpirydate"));
            request.setDoctorDetails(details);
        }
        // Add other roles
    }

    private String getCellStringValue(Row row, Map<String, Integer> headers, String columnName) {
        Integer index = headers.get(columnName.toLowerCase());
        if (index == null) return null;

        Cell cell = row.getCell(index);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }
    private LocalDate getCellDateValue(Row row, Map<String, Integer> headers, String columnName) {
        Integer index = headers.get(columnName.toLowerCase());
        if (index == null) return null;

        Cell cell = row.getCell(index);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        return null;
    }

    private Map<String, Integer> mapHeaders(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim().toLowerCase(), i);
        }
        return map;
    }

    private Map<String, Integer> mapExcelHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim().toLowerCase();
            map.put(header, cell.getColumnIndex());
        }
        return map;
    }
    private String getStringValue(String[] row, Map<String, Integer> headers, String columnName) {
        Integer index = headers.get(columnName.toLowerCase());
        if (index == null || index >= row.length) return null;
        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }
}

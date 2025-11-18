package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.CreateUserRequest;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
    private Map<String, Integer> mapHeaders(String[] headers) {
        Map<String, Integer> headerMap = new HashMap<>();
        for(int i = 0; i<headers.length; i++){
            headerMap.put(headers[i].trim().toLowerCase(), i);
        }
        return headerMap;
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

    private void parseRoleSpecificDetails(CreateUserRequest request, String[] row, Map<String, Integer> headers) {
        String role = request.getRole();

        if ("DOCTOR".equals(role)) {
            CreateUserRequest.DoctorDetailsRequest details = new CreateUserRequest.DoctorDetailsRequest();
            details.setSpecialization(getStringValue(row, headers, "specialization"));
            details.setDepartment(getStringValue(row, headers, "department"));
            details.setLicenseNumber(getStringValue(row, headers, "licensenumber"));
            details.setLicenseAuthority(getStringValue(row, headers, "licenseauthority"));
            details.setLicenseIssueDate(getDateValue(row, headers, "licenseissuedate"));
            details.setLicenseExpiryDate(getDateValue(row, headers, "licenseexpirydate"));
            request.setDoctorDetails(details);

        } else if ("NURSE".equals(role)) {
            CreateUserRequest.NurseDetailsRequest details = new CreateUserRequest.NurseDetailsRequest();
            details.setSpecialization(getStringValue(row, headers, "specialization"));
            details.setDepartment(getStringValue(row, headers, "department"));
            details.setLicenseNumber(getStringValue(row, headers, "licensenumber"));
            details.setShiftHours(getStringValue(row, headers, "shifthours"));
            details.setYearsOfExperience(getIntValue(row, headers, "yearsofexperience"));
            details.setLicenseIssueDate(getDateValue(row, headers, "licenseissuedate"));
            details.setLicenseExpiryDate(getDateValue(row, headers, "licenseexpirydate"));
            request.setNurseDetails(details);

        } else if ("PHARMACIST".equals(role)) {
            CreateUserRequest.PharmacistDetailsRequest details = new CreateUserRequest.PharmacistDetailsRequest();
            details.setSpecialization(getStringValue(row, headers, "specialization"));
            details.setDepartment(getStringValue(row, headers, "department"));
            details.setLicenseNumber(getStringValue(row, headers, "licensenumber"));
            details.setLicenseAuthority(getStringValue(row, headers, "licenseauthority"));
            details.setYearsOfExperience(getIntValue(row, headers,"yearsofexperience"));
            details.setLicenseIssueDate(getDateValue(row, headers, "licenseissuedate"));
            details.setLicenseExpiryDate(getDateValue(row, headers, "licenseexpirydate"));
            request.setPharmacistDetails(details);

        }else if ("LAB_SCIENTIST".equals(role)) {
            CreateUserRequest.LabScientistDetailsRequest details = new CreateUserRequest.LabScientistDetailsRequest();
            details.setSpecialization(getStringValue(row, headers, "specialization"));
            details.setDepartment(getStringValue(row, headers, "department"));
            details.setLicenseNumber(getStringValue(row, headers, "licensenumber"));
            details.setLicenseAuthority(getStringValue(row, headers, "licenseauthority"));
            details.setLicenseIssueDate(getDateValue(row, headers, "licenseissuedate"));
            details.setLicenseExpiryDate(getDateValue(row, headers, "licenseexpirydate"));
            details.setYearsOfExperience(getIntValue(row, headers,"yearsofexperience"));
            request.setLabScientistDetails(details);

        }
        // Add other roles as needed
    }

    private String getStringValue(String[] row, Map<String, Integer> headers, String columnName) {
        Integer index = headers.get(columnName.toLowerCase());
        if (index == null || index >= row.length) return null;
        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }
    private Integer getIntValue(String[] row, Map<String, Integer> headers, String columnName) {
       String value = getStringValue(row, headers, columnName);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private LocalDate getDateValue(String[] row, Map<String, Integer> headers, String columnName) {
        String value = getStringValue(row, headers, columnName);
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        }catch (Exception e){
            log.warn("Invalid date format: {}", value);
            return null;
        }
    }


}

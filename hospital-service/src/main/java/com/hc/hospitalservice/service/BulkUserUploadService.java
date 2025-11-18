package com.hc.hospitalservice.service;

import com.hc.hospitalservice.dto.BulkUploadResponse;
import com.hc.hospitalservice.dto.CreateUserRequest;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BulkUserUploadService {
    private final UserManagementService userManagementService;
    public BulkUploadResponse processBulkUpload(MultipartFile file, String tenantDbName, String hospitalId) throws IOException {
        String filename = file.getOriginalFilename();
        List<CreateUserRequest> userRequests;
        if (filename != null && filename.endsWith(".csv")) {
            userRequests = parseCSV(file);
        } else if (filename != null && filename.endsWith(".xlsx")) {
            userRequests = parseExcel(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format");
        }
        List<BulkUploadResponse.FailureDetail> validationErrors = validateRecords(userRequests, tenantDbName);

        if (!validationErrors.isEmpty()) {
            log.warn("⚠️ Validation failed for {} records", validationErrors.size());
            return BulkUploadResponse.builder()
                    .totalRecords(userRequests.size())
                    .successful(0)
                    .failed(validationErrors.size())
                    .failures(validationErrors)
                    .message("Validation failed. Please fix errors and re-upload.")
                    .build();
        }

        // Process each user
        return processUsers(userRequests, tenantDbName, Integer.valueOf(hospitalId));

    }

    private List<CreateUserRequest> parseExcel(MultipartFile file) throws IOException {
        List<CreateUserRequest> users = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            // Process each sheet (one per role)
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName().toUpperCase();

                log.info(" Processing sheet: {}", sheetName);

                // Determine role from sheet name
                String role = determineRoleFromSheetName(sheetName);
                if (role == null) {
                    log.warn(" Skipping sheet: {} (unknown role)", sheetName);
                    continue;
                }

                // First row is header
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                Map<String, Integer> headerMap = mapExcelHeaders(headerRow);

                // Process data rows
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    try {
                        CreateUserRequest user = parseUserFromExcelRow(row, headerMap, role, rowIndex + 1);
                        users.add(user);
                    } catch (Exception e) {
                        log.warn("️ Skipping row {} in sheet {}: {}", rowIndex + 1, sheetName, e.getMessage());
                    }
                }
            }
        }

        return users;
    }

    private CreateUserRequest parseUserFromExcelRow(Row row, Map<String, Integer> headers, String role, int rowNumber) {
        CreateUserRequest request = new CreateUserRequest();

        request.setFirstName(getCellStringValue(row, headers, "firstname"));
        request.setMiddleName(getCellStringValue(row, headers, "middlename"));
        request.setLastName(getCellStringValue(row, headers, "lastname"));
        request.setEmail(getCellStringValue(row, headers, "email"));
        request.setPhoneNumber(getCellStringValue(row, headers, "phonenumber"));
        request.setRole(role);

        // Parse role-specific details
        parseRoleSpecificDetailsFromExcel(request, row, headers);

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
    private String determineRoleFromSheetName(String sheetName) {
        if (sheetName.contains("DOCTOR")) return "DOCTOR";
        if (sheetName.contains("NURSE")) return "NURSE";
        if (sheetName.contains("PATIENT")) return "PATIENT";
        if (sheetName.contains("PHARMACIST")) return "PHARMACIST";
        if (sheetName.contains("LAB")) return "LAB_SCIENTIST";
        return null;
    }


    private Map<String, Integer> mapExcelHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = cell.getStringCellValue().trim().toLowerCase();
            map.put(header, cell.getColumnIndex());
        }
        return map;
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
        // Add other roles as needed(hospital staff would need to be here when i figure it out)
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
    private List<BulkUploadResponse.FailureDetail> validateRecords(List<CreateUserRequest> users, String tenantDb) {
        List<BulkUploadResponse.FailureDetail> errors = new ArrayList<>();
        Set<String> emails = new HashSet<>();

        for (int i = 0; i < users.size(); i++) {
            CreateUserRequest user = users.get(i);
            int rowNumber = i + 2; // +2 because: 0-indexed + header row

            // Check required fields
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                errors.add(new BulkUploadResponse.FailureDetail(
                        rowNumber, "", "Email is required"));
                continue;
            }

            if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
                errors.add(new BulkUploadResponse.FailureDetail(
                        rowNumber, user.getEmail(), "First name is required"));
                continue;
            }

            if (user.getLastName() == null || user.getLastName().isEmpty()) {
                errors.add(new BulkUploadResponse.FailureDetail(
                        rowNumber, user.getEmail(), "Last name is required"));
                continue;
            }

            if (user.getRole() == null) {
                errors.add(new BulkUploadResponse.FailureDetail(
                        rowNumber, user.getEmail(), "Role is required"));
                continue;
            }

            // Check for duplicates in file
            if (!emails.add(user.getEmail())) {
                errors.add(new BulkUploadResponse.FailureDetail(
                        rowNumber, user.getEmail(), "Duplicate email in file"));
            }
        }
        return errors;
    }
    private BulkUploadResponse processUsers(List<CreateUserRequest> users, String tenantDb, Integer hospitalId) {
        int successful = 0;
        List<BulkUploadResponse.FailureDetail> failures = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            CreateUserRequest user = users.get(i);
            int rowNumber = i + 2;

            try {
               userManagementService.createUser(user, tenantDb, hospitalId);
                successful++;
                log.info(" Row {}: Invited {}", rowNumber, user.getEmail());

            } catch (Exception e) {
                log.error(" Row {}: Failed to invite {}", rowNumber, user.getEmail(), e);
                failures.add(new BulkUploadResponse.FailureDetail(
                        rowNumber, user.getEmail(), e.getMessage()));
            }
        }

        return BulkUploadResponse.builder()
                .totalRecords(users.size())
                .successful(successful)
                .failed(failures.size())
                .failures(failures)
                .message(String.format("Processed %d users. %d successful, %d failed.",
                        users.size(), successful, failures.size()))
                .build();
    }


}

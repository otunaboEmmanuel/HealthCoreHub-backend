package com.hc.hospitalservice.service;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class TemplateDownloadService {

    public ByteArrayResource generateCSVTemplate() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            // Headers
            String[] headers = {
                    "firstName", "middleName", "lastName", "email", "phoneNumber", "role",
                    "specialization", "department", "licenseNumber", "licenseAuthority",
                    "licenseIssueDate", "licenseExpiryDate", "shiftHours", "yearsOfExperience"
            };
            writer.writeNext(headers);

            // Example rows
            String[] doctorExample = {
                    "John", "Michael", "Doe", "john.doe@example.com", "+2348012345678", "DOCTOR",
                    "Cardiology", "Heart Center", "DOC-12345", "Medical Board",
                    "2020-01-15", "2030-01-15", "", ""
            };
            writer.writeNext(doctorExample);

            String[] nurseExample = {
                    "Jane", "Mary", "Smith", "jane.smith@example.com", "+2348023456789", "NURSE",
                    "Pediatric", "Children Ward", "NUR-67890", "Nursing Council",
                    "2019-05-20", "2029-05-20", "Day Shift", "5"
            };
            writer.writeNext(nurseExample);

            String[] pharmacistExample = {
                    "Robert", "", "Johnson", "robert.j@example.com", "+2348034567890", "PHARMACIST",
                    "Clinical Pharmacy", "Pharmacy Dept", "PHARM-11111", "Pharmacy Board",
                    "2021-03-10", "2031-03-10", "", "3"
            };
            writer.writeNext(pharmacistExample);

            // Instructions as comments
            writer.writeNext(new String[]{""});
            writer.writeNext(new String[]{"# INSTRUCTIONS FOR CSV UPLOAD:"});
            writer.writeNext(new String[]{"# 1. Required fields: firstName, lastName, email, phoneNumber, role"});
            writer.writeNext(new String[]{"# 2. Valid roles: DOCTOR, NURSE, PHARMACIST, LAB_SCIENTIST, PATIENT, ADMIN, STAFF"});
            writer.writeNext(new String[]{"# 3. Date format: YYYY-MM-DD (e.g., 2020-01-15)"});
            writer.writeNext(new String[]{"# 4. Role-specific fields:"});
            writer.writeNext(new String[]{"#    - DOCTOR: specialization, department, licenseNumber, licenseAuthority, dates"});
            writer.writeNext(new String[]{"#    - NURSE: specialization, department, licenseNumber, shiftHours, yearsOfExperience, dates"});
            writer.writeNext(new String[]{"#    - PHARMACIST: specialization, department, licenseNumber, licenseAuthority, yearsOfExperience, dates"});
            writer.writeNext(new String[]{"# 5. Empty fields are allowed - users can complete profiles during activation"});
            writer.writeNext(new String[]{"# 6. NO password field - users set passwords via activation email link"});
            writer.writeNext(new String[]{"# 7. Delete example rows and instructions before uploading"});
        }

        return new ByteArrayResource(outputStream.toByteArray());
    }

    public ByteArrayResource generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create sheets for each role
            createDoctorSheet(workbook);
            createNurseSheet(workbook);
            createPharmacistSheet(workbook);
            createLabScientistSheet(workbook);
            createGeneralStaffSheet(workbook);
            createInstructionsSheet(workbook);

            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        }
    }

    private void createDoctorSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Doctors");

        // Style for headers
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);

        // Headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "firstName", "middleName", "lastName", "email", "phoneNumber",
                "specialization", "department", "licenseNumber", "licenseAuthority",
                "licenseIssueDate", "licenseExpiryDate"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        // Example row
        Row exampleRow = sheet.createRow(1);
        String[] example = {
                "John", "Michael", "Doe", "john.doe@example.com", "+2348012345678",
                "Cardiology", "Heart Center", "DOC-12345", "Medical Board",
                "2020-01-15", "2030-01-15"
        };

        for (int i = 0; i < example.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(example[i]);
            cell.setCellStyle(exampleStyle);
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);
    }

    private void createNurseSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Nurses");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "firstName", "middleName", "lastName", "email", "phoneNumber",
                "specialization", "department", "licenseNumber",
                "licenseIssueDate", "licenseExpiryDate", "shiftHours", "yearsOfExperience"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        Row exampleRow = sheet.createRow(1);
        String[] example = {
                "Jane", "Mary", "Smith", "jane.smith@example.com", "+2348023456789",
                "Pediatric", "Children Ward", "NUR-67890",
                "2019-05-20", "2029-05-20", "Day Shift", "5"
        };

        for (int i = 0; i < example.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(example[i]);
            cell.setCellStyle(exampleStyle);
        }

        sheet.createFreezePane(0, 1);
    }

    private void createPharmacistSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Pharmacists");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "firstName", "middleName", "lastName", "email", "phoneNumber",
                "specialization", "department", "licenseNumber", "licenseAuthority",
                "licenseIssueDate", "licenseExpiryDate", "yearsOfExperience"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        Row exampleRow = sheet.createRow(1);
        String[] example = {
                "Robert", "", "Johnson", "robert.j@example.com", "+2348034567890",
                "Clinical Pharmacy", "Pharmacy Dept", "PHARM-11111", "Pharmacy Board",
                "2021-03-10", "2031-03-10", "3"
        };

        for (int i = 0; i < example.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(example[i]);
            cell.setCellStyle(exampleStyle);
        }

        sheet.createFreezePane(0, 1);
    }

    private void createLabScientistSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Lab Scientists");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "firstName", "middleName", "lastName", "email", "phoneNumber",
                "specialization", "department", "licenseNumber", "licenseAuthority",
                "licenseIssueDate", "licenseExpiryDate", "yearsOfExperience"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        Row exampleRow = sheet.createRow(1);
        String[] example = {
                "Sarah", "Ann", "Williams", "sarah.w@example.com", "+2348045678901",
                "Microbiology", "Laboratory", "LAB-22222", "Medical Lab Board",
                "2018-07-01", "2028-07-01", "7"
        };

        for (int i = 0; i < example.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(example[i]);
            cell.setCellStyle(exampleStyle);
        }

        sheet.createFreezePane(0, 1);
    }

    private void createGeneralStaffSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Admin & Staff");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "firstName", "middleName", "lastName", "email", "phoneNumber", "role"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000);
        }

        Row example1 = sheet.createRow(1);
        String[] data1 = {
                "Alice", "", "Brown", "alice.b@example.com", "+2348056789012", "ADMIN"
        };
        for (int i = 0; i < data1.length; i++) {
            Cell cell = example1.createCell(i);
            cell.setCellValue(data1[i]);
            cell.setCellStyle(exampleStyle);
        }

        Row example2 = sheet.createRow(2);
        String[] data2 = {
                "Bob", "Lee", "Green", "bob.g@example.com", "+2348067890123", "STAFF"
        };
        for (int i = 0; i < data2.length; i++) {
            Cell cell = example2.createCell(i);
            cell.setCellValue(data2[i]);
            cell.setCellStyle(exampleStyle);
        }

        sheet.createFreezePane(0, 1);
    }

    private void createInstructionsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("📋 Instructions");

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFont(titleFont);

        CellStyle instructionStyle = workbook.createCellStyle();
        instructionStyle.setWrapText(true);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BULK USER UPLOAD INSTRUCTIONS");
        titleCell.setCellStyle(titleStyle);

        rowNum++; // Empty row

        // Instructions
        String[] instructions = {
                "GENERAL INSTRUCTIONS:",
                "1. Each sheet is for a specific role (Doctors, Nurses, Pharmacists, Lab Scientists, Admin/Staff)",
                "2. Fill in the appropriate sheet for the users you want to add",
                "3. Delete the example rows before uploading",
                "4. Save the file and upload it through the admin panel",
                "",
                "REQUIRED FIELDS:",
                "• firstName, lastName, email, phoneNumber",
                "• role (DOCTOR, NURSE, PHARMACIST, LAB_SCIENTIST, ADMIN, STAFF)",
                "",
                "OPTIONAL FIELDS:",
                "• middleName",
                "• All role-specific fields (specialization, department, license info, etc.)",
                "• Users can complete missing information during account activation",
                "",
                "DATE FORMAT:",
                "• Use YYYY-MM-DD format (e.g., 2020-01-15)",
                "",
                "IMPORTANT NOTES:",
                "• NO password field - users will set passwords via activation email",
                "• Activation links expire in 24 hours",
                "• Each email must be unique",
                "• Phone numbers should include country code (e.g., +234...)",
                "",
                "ROLE-SPECIFIC FIELDS:",
                "• DOCTOR: specialization, department, licenseNumber, licenseAuthority, dates",
                "• NURSE: specialization, department, licenseNumber, shiftHours, yearsOfExperience, dates",
                "• PHARMACIST: specialization, department, licenseNumber, licenseAuthority, yearsOfExperience, dates",
                "• LAB_SCIENTIST: specialization, department, licenseNumber, licenseAuthority, yearsOfExperience, dates",
                "• ADMIN/STAFF: No additional fields required",
                "",
                "AFTER UPLOAD:",
                "• Users will receive an activation email",
                "• They must click the link and set their password within 24 hours",
                "• Their account status will be PENDING_ACTIVATION until they complete this step",
                "",
                "SUPPORT:",
                "• Contact your system administrator if you encounter any issues",
                "• Make sure to test with a small batch first before uploading large files"
        };

        for (String instruction : instructions) {
            Row row = sheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue(instruction);
            cell.setCellStyle(instructionStyle);
            row.setHeightInPoints(20);
        }

        sheet.setColumnWidth(0, 15000);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createExampleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }
}